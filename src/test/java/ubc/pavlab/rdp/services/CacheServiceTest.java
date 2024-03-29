package ubc.pavlab.rdp.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
public class CacheServiceTest {

    @TestConfiguration
    static class CacheServiceTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings a = new ApplicationSettings();
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setEnabled( true );
            a.setCache( cacheSettings );
            return a;
        }

        @Bean
        public CacheService cacheService() {
            return new CacheService();
        }
    }

    @Autowired
    private CacheService cacheService;

    @MockBean
    private GeneInfoService geneService;

    @MockBean
    private UserGeneService userGeneService;

    @MockBean
    private GOService goService;

    @MockBean
    private OrganInfoService organInfoService;


    @MockBean
    private UserOrganService userOrganService;

    @MockBean
    private UserService userService;

    @MockBean
    private OntologyService ontologyService;

    @MockBean
    private ReactomeService reactomeService;

    @Test
    public void updateCache_thenSucceed() throws ReactomeException {
        cacheService.updateCache();
        verify( geneService ).updateGenes();
        verify( geneService ).updateGeneOrthologs();
        verify( userGeneService ).updateUserGenes();
        verify( goService ).updateGoTerms();
        verify( userService ).updateUserTerms();
        verify( organInfoService ).updateOrganInfos();
        verify( userOrganService ).updateUserOrgans();
        verify( reactomeService ).updatePathwaysOntology();
        verify( reactomeService ).updatePathwaySummations( any() );
    }

}
