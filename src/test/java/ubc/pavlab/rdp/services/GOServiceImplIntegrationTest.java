package ubc.pavlab.rdp.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class GOServiceImplIntegrationTest {

    @TestConfiguration
    static class GOServiceImplTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings a = new ApplicationSettings();
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setEnabled( true );
            cacheSettings.setLoadFromDisk( false );
            a.setCache( cacheSettings );
            return a;
        }

        @Bean
        public GOService goService() {
            return new GOServiceImpl();
        }

        @Bean
        public OBOParser oboParser() {
            return new OBOParser();
        }

    }

    @MockBean
    private UserService userService;

    @MockBean
    private GeneInfoService geneInfoService;

    @MockBean
    private TaxonService taxonService;

    @Autowired
    GOService goService;

    @Test
    public void updateGoTerms() {
        goService.updateGoTerms();
        assertThat( goService.getTerm( "GO:0000001" ) ).isNotNull();
    }
}
