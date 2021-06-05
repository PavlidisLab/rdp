package ubc.pavlab.rdp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
public class RemoteResourceServiceTest {

    @TestConfiguration
    static class RemoteResourceServiceTestContextConfiguration {

        @Bean
        public RemoteResourceService remoteResourceService() {
            return new RemoteResourceServiceImpl();
        }

        @Bean
        public AsyncRestTemplate asyncRestTemplate() {
            return new AsyncRestTemplate();
        }

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private RemoteResourceService remoteResourceService;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.InternationalSearchSettings iSearchSettings;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleRepository roleRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Before
    public void setUp() {
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com" } );
    }

    @Test
    public void getVersion_thenReturnSuccess() throws JsonProcessingException, RemoteException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.3.2" ) ) ) ) );
        assertThat( remoteResourceService.getApiVersion( URI.create( "http://example.com/" ) ) ).isEqualTo( "1.3.2" );
        mockServer.verify();
    }

    @Test
    public void getVersion_whenEndpointReturnPre14Response_thenAssume100() throws RemoteException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( "{\"message\":\"This is this applications API. Please see documentation.\",\"version\":\"1.0.0\"}" ) );
        assertThat( remoteResourceService.getApiVersion( URI.create( "http://example.com/" ) ) ).isEqualTo( "1.0.0" );
        mockServer.verify();
    }

    @Test
    public void findUserByLikeName_thenReturnSuccess() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{} ) ) );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null, null );
        mockServer.verify();
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void findUserByLikeName_whenAdmin_thenReturnSuccess() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true&auth=1234" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{} ) ) );
        Role adminRole = createRole( 1, "ROLE_ADMIN" );
        User adminUser = createUserWithRoles( 1, adminRole );
        when( iSearchSettings.getSearchToken() ).thenReturn( "1234" );
        when( roleRepository.findByRole( "ROLE_ADMIN" ) ).thenReturn( adminRole );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null, null );
        mockServer.verify();
    }

    @Test
    @Ignore("There an issue with with multiple requests performed against Spring MockServer.")
    public void findGenesBySymbol_whenTier3_isRestricted() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new UserGene[]{} ) ) );
        mockServer.expect( requestTo( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER2" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new UserGene[]{} ) ) );
        Taxon taxon = createTaxon( 9606 );
        remoteResourceService.findGenesBySymbol( "ok", taxon, EnumSet.of( TierType.TIER1, TierType.TIER2, TierType.TIER3 ), null, null, null, null );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_thenReturnSuccess() throws RemoteException, JsonProcessingException {
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenAuthorityIsKnownButSchemeDiffers_thenReturnSuccess() throws RemoteException, JsonProcessingException {
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "https://example.com" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenAuthorityIsKnownButPathDiffers_thenReturnSuccess() throws URISyntaxException, RemoteException, JsonProcessingException {
        User user = createRemoteUser( 1, new URI( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/foo" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenRemoteHostUriContainsPath_thenRelativizeWithEndpoint_thenReturnSuccess() throws RemoteException, JsonProcessingException {
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "https://example.com/rgr/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "https://example.com/rgr" } );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "https://example.com/rgr" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenRemoteHostUriContainsPath_thenReplaceWithEndpoint_thenReturnSuccess() throws RemoteException, JsonProcessingException {
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com/" } );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test(expected = RemoteException.class)
    public void getRemoteUser_whenRemoteDoesNotExist_thenRaiseException() throws RemoteException {
        remoteResourceService.getRemoteUser( 1, URI.create( "http://example1.com" ) );
    }

}
