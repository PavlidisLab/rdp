package ubc.pavlab.rdp.services;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
public class RemoteResourceServiceTest {

    @TestConfiguration
    static class RemoteResourceServiceTestContextConfiguration {

        @Bean
        public RemoteResourceService remoteResourceService() {
            return new RemoteResourceServiceImpl();
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

    @MockBean
    private ResteasyClient resteasyClient;

    @Mock
    private Response response;

    @Before
    public void setUp() {
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com" } );

        ResteasyWebTarget resteasyWebTarget = mock( ResteasyWebTarget.class );
        Invocation.Builder builder = mock( Invocation.Builder.class );
        when( builder.get() ).thenReturn( response );
        when( resteasyWebTarget.request() ).thenReturn( builder );
        when( resteasyClient.target( any( URI.class ) ) ).thenReturn( resteasyWebTarget );
    }

    @Test
    public void findUserByLikeName_thenReturnSuccess() throws URISyntaxException {
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User[].class ) ).thenReturn( new User[]{} );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/search?nameLike=ok&prefix=true" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void findUserByLikeName_whenAdmin_thenReturnSuccess() throws URISyntaxException {
        Role adminRole = createRole( 1, "ROLE_ADMIN" );
        User adminUser = createUserWithRoles( 1, adminRole );
        when( iSearchSettings.getSearchToken() ).thenReturn( "1234" );
        when( roleRepository.findByRole( "ROLE_ADMIN" ) ).thenReturn( adminRole );
        when( userService.findCurrentUser() ).thenReturn( adminUser );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User[].class ) ).thenReturn( new User[]{} );
        remoteResourceService.findUsersByLikeName( "ok", true, null, null );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/search?nameLike=ok&prefix=true&auth=1234" ) );
    }

    @Test
    public void findGenesBySymbol_whenTier3_isRestricted() throws URISyntaxException, RemoteException {
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( UserGene[].class ) ).thenReturn( new UserGene[]{} );

        Taxon taxon = createTaxon( 9606 );
        remoteResourceService.findGenesBySymbol( "ok", taxon, EnumSet.of( TierType.TIER1, TierType.TIER2, TierType.TIER3 ), null, null, null );
        verify( resteasyClient ).target( new URI( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER1" ) );
        verify( resteasyClient ).target( new URI( "http://example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER2" ) );
        verifyNoMoreInteractions( resteasyClient );
    }

    @Test
    public void getRemoteUser_thenReturnSuccess() throws URISyntaxException, RemoteException {
        User user = createUser( 1 );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User.class ) ).thenReturn( user );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com" ) );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/1" ) );
        assertThat( remoteUser ).isEqualTo( user );
    }

    @Test
    public void getRemoteUser_whenAuthorityIsKnownButSchemeDiffers_thenReturnSuccess() throws URISyntaxException, RemoteException {
        User user = createUser( 1 );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User.class ) ).thenReturn( user );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "https://example.com" ) );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/1" ) );
        assertThat( remoteUser ).isEqualTo( user );
    }

    @Test
    public void getRemoteUser_whenAuthorityIsKnownButPathDiffers_thenReturnSuccess() throws URISyntaxException, RemoteException {
        User user = createUser( 1 );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User.class ) ).thenReturn( user );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/foo" ) );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/1" ) );
        assertThat( remoteUser ).isEqualTo( user );
    }

    @Test
    public void getRemoteUser_whenRemoteHostUriContainsPath_thenRelativizeWithEndpoint_thenReturnSuccess() throws URISyntaxException, RemoteException {
        User user = createUser( 1 );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User.class ) ).thenReturn( user );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "https://example.com/rgr" } );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "https://example.com/rgr" ) );
        verify( resteasyClient ).target( new URI( "https://example.com/rgr/api/users/1" ) );
        assertThat( remoteUser ).isEqualTo( user );
    }

    @Test
    public void getRemoteUser_whenRemoteHostUriContainsPath_thenReplaceWithEndpoint_thenReturnSuccess() throws URISyntaxException, RemoteException {
        User user = createUser( 1 );
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User.class ) ).thenReturn( user );
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "http://example.com/" } );
        User remoteUser = remoteResourceService.getRemoteUser( 1, URI.create( "http://example.com/" ) );
        verify( resteasyClient ).target( new URI( "http://example.com/api/users/1" ) );
        assertThat( remoteUser ).isEqualTo( user );
    }

    @Test(expected = RemoteException.class)
    public void getRemoteUser_whenRemoteDoesNotExist_thenRaiseException() throws RemoteException {
        remoteResourceService.getRemoteUser( 1, URI.create( "http://example1.com" ) );
    }
}
