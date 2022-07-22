package ubc.pavlab.rdp.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.ExpectedCount;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ubc.pavlab.rdp.util.TestUtils.createRemoteUser;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

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
    private UserGeneService userGeneService;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private TaxonService taxonService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    /* fixtures */
    private User adminUser;
    private Role adminRole;

    @Before
    public void setUp() {
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com" } );
        // admin user used for remote admin search
        adminUser = new User();
        adminUser.setId( 1 );
        adminRole = new Role();
        adminRole.setId( 1 );
        adminRole.setRole( "ADMIN" );
        adminUser.getRoles().add( adminRole );
        when( roleRepository.findByRole( "ROLE_ADMIN" ) ).thenReturn( adminRole );
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
    public void getVersion_whenEndpointReturnEarly14Response_thenAssume140() throws RemoteException, JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "v0" ) ) ) ) );
        assertThat( remoteResourceService.getApiVersion( URI.create( "http://example.com/" ) ) ).isEqualTo( "1.4.0" );
        mockServer.verify();
    }

    @Test
    public void findUserByLikeName_thenReturnSuccess() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.0.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{} ) ) );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null, null, null );
        mockServer.verify();
    }

    @Test
    public void findUserByLikeName_whenAdmin_thenReturnSuccess() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.0.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true&auth=1234" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{} ) ) );
        when( iSearchSettings.getSearchToken() ).thenReturn( "1234" );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null, null, null );
        mockServer.verify();
    }

    @Test
    public void findUsersByLikeNameAndDescription() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.5.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true&descriptionLike=ok2" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{ createRemoteUser( 1, URI.create( "http://example.com" ) ) } ) ) );
        assertThat( remoteResourceService.findUsersByLikeNameAndDescription( "ok", true, "ok2", null, null, null, null ) )
                .hasSize( 1 )
                .extracting( "id" )
                .containsExactly( 1 );
        mockServer.verify();
    }

    @Test
    public void findUsersByLikeNameAndDescription_whenVersionIsPre15_thenPerformTwoQueriesAndIntersectTheirResults() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.4.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{ createRemoteUser( 1, URI.create( "http://example.com" ) ), createRemoteUser( 2, URI.create( "http://example.com" ) ) } ) ) );
        mockServer.expect( requestTo( "http://example.com/api/users/search?descriptionLike=ok2" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new User[]{ createRemoteUser( 2, URI.create( "http://example.com" ) ), createRemoteUser( 3, URI.create( "http://example.com" ) ) } ) ) );
        mockServer.expect( ExpectedCount.never(), requestTo( "http://example.com/api/users/search?nameLike=ok&prefix=true&descriptionLike=ok2" ) );
        assertThat( remoteResourceService.findUsersByLikeNameAndDescription( "ok", true, "ok2", null, null, null, null ) )
                .extracting( "id" )
                .containsExactly( 2 );
        mockServer.verify();
    }

    @Test
    public void findGenesBySymbol_whenTier3_isRestricted() throws JsonProcessingException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.0.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new UserGene[]{} ) ) );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.0.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER2" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new UserGene[]{} ) ) );
        Taxon taxon = createTaxon( 9606 );
        remoteResourceService.findGenesBySymbol( "ok", taxon, EnumSet.of( TierType.TIER1, TierType.TIER2, TierType.TIER3 ), null, null, null, null, null );
        mockServer.verify();
    }

    @Test
    public void findGenesBySymbol_whenRequestTimeoutIsSet_thenSucceed() throws JsonProcessingException {
        when( iSearchSettings.getRequestTimeout() ).thenReturn( 3 );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.0.0" ) ) ) ) );
        mockServer.expect( requestTo( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new UserGene[]{} ) ) );
        Taxon taxon = createTaxon( 9606 );
        remoteResourceService.findGenesBySymbol( "ok", taxon, EnumSet.of( TierType.TIER1 ), null, null, null, null, null );
        mockServer.verify();
        // TODO: this test case does not actually check if Future.get() is invoked with a timeout, but I haven't found a
        // good way to do that insofar
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

    @Test
    public void getAnonymizedUser_whenEndpointHasAPreReleaseVersion_thenReturnSuccess() throws JsonProcessingException, RemoteException {
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( new OpenAPI().info( new Info().version( "1.4.0-SNAPSHOT" ) ) ) ) );
        UUID uuid = UUID.randomUUID();
        User user = createRemoteUser( 1, URI.create( "http://example.com" ) );
        mockServer.expect( requestTo( "http://example.com/api/users/by-anonymous-id/" + uuid ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        assertThat( remoteResourceService.getAnonymizedUser( uuid, URI.create( "http://example.com/" ) ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "email", user.getEmail() );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenSearchTokenIsSetAndUserIsAdmin_thenUseTheSearchToken() throws JsonProcessingException, RemoteException {
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com/" } );
        when( iSearchSettings.getSearchToken() ).thenReturn( "abcd" );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1?auth=abcd" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenSearchTokenIsSetAndUserIsAdminAndRemotePartnerHasNoAuth_thenDoNotLeakSearchTokenNorTheNoAuthQueryParameter() throws JsonProcessingException, RemoteException {
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com?noauth" } );
        when( iSearchSettings.getSearchToken() ).thenReturn( "abcd" );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenSearchTokenIsSetAndUserIsNotAdmin_thenDontLeakTheSearchToken() throws JsonProcessingException, RemoteException {
        when( iSearchSettings.getSearchToken() ).thenReturn( "abcd" );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com" } );
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenSpecificSearchTokenIsSetAndUserIsAdmin_thenUseSpecificToken() throws JsonProcessingException, RemoteException {
        when( iSearchSettings.getSearchToken() ).thenReturn( "abcd" );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com?auth=efgh" } );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1?auth=efgh" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }

    @Test
    public void getRemoteUser_whenSpecificSearchTokenIsSetAndUserIsNotAdmin_thenDontLeakSpecificToken() throws JsonProcessingException, RemoteException {
        when( iSearchSettings.getSearchToken() ).thenReturn( "abcd" );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com?auth=efgh" } );
        User user = createRemoteUser( 1, URI.create( "http://example.com/api/users/1" ) );
        MockRestServiceServer mockServer = MockRestServiceServer.createServer( asyncRestTemplate );
        mockServer.expect( requestTo( "http://example.com/api/users/1" ) )
                .andRespond( withStatus( HttpStatus.OK )
                        .contentType( MediaType.APPLICATION_JSON )
                        .body( objectMapper.writeValueAsString( user ) ) );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        assertThat( remoteUser ).isEqualTo( user );
        mockServer.verify();
    }
}
