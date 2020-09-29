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
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.util.EnumSet;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

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
        when( iSearchSettings.getApis() ).thenReturn( new String[]{ "example.com" } );

        ResteasyWebTarget resteasyWebTarget = mock( ResteasyWebTarget.class );
        Invocation.Builder builder = mock( Invocation.Builder.class );
        when( builder.get() ).thenReturn( response );
        when( resteasyWebTarget.request() ).thenReturn( builder );
        when( resteasyClient.target( any( String.class ) ) ).thenReturn( resteasyWebTarget );
    }

    @Test
    public void findUserByLikeName_thenReturnSuccess() {
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( User[].class ) ).thenReturn( new User[]{} );
        remoteResourceService.findUsersByLikeName( "ok", true, Optional.empty(), Optional.empty() );
        verify( resteasyClient ).target( "example.com/api/users/search?nameLike=ok&prefix=true" );
    }

    @Test
    public void findGenesBySymbol_whenTier3_isRestricted() {
        when( response.getStatus() ).thenReturn( 200 );
        when( response.readEntity( UserGene[].class ) ).thenReturn( new UserGene[]{} );

        Taxon taxon = createTaxon( 9606 );
        remoteResourceService.findGenesBySymbol( "ok", taxon, EnumSet.of( TierType.TIER1, TierType.TIER2, TierType.TIER3 ), null, Optional.empty(), Optional.empty() );
        verify( resteasyClient ).target( "example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER1" );
        verify( resteasyClient ).target( "example.com/api/genes/search?symbol=ok&taxonId=9606&tier=TIER2" );
        verifyNoMoreInteractions( resteasyClient );
    }
}
