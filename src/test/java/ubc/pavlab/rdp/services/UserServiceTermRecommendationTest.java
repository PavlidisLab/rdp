package ubc.pavlab.rdp.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.*;
import ubc.pavlab.rdp.security.SecureTokenChallenge;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;

import javax.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A tailored test to verify that GO recommendation work as expected.
 *
 * @author poirigui
 */
@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
public class UserServiceTermRecommendationTest {

    @TestConfiguration
    static class TTCC {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
            when( applicationSettings.getIsearch() ).thenReturn( new ApplicationSettings.InternationalSearchSettings() );
            when( applicationSettings.getGoTermSizeLimit() ).thenReturn( 50L );
            when( applicationSettings.getGoTermMinOverlap() ).thenReturn( 2L );
            when( applicationSettings.getEnabledTiers() ).thenReturn( EnumSet.allOf( TierType.class ) );
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setTermFile( "classpath:cache/go.obo" );
            cacheSettings.setAnnotationFile( new ClassPathResource( "cache/gene2go.gz" ) );
            when( applicationSettings.getCache() ).thenReturn( cacheSettings );
            return applicationSettings;
        }

        @Bean
        public GOService goService() {
            return new GOServiceImpl();
        }

        @Bean
        public GeneOntologyTermInfoRepository geneOntologyTermInfoRepository() {
            return new GeneOntologyTermInfoRepository();
        }

        @Bean
        public UserServiceImpl userService() {
            return new UserServiceImpl();
        }

        @Bean
        public OBOParser oboParser() {
            return new OBOParser();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private RoleRepository roleRepository;
    @MockBean
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean
    private VerificationTokenRepository tokenRepository;
    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @MockBean
    private OrganInfoService organInfoService;
    @MockBean
    private ApplicationEventPublisher eventPublisher;
    @MockBean
    private AccessTokenRepository accessTokenRepository;
    @MockBean
    private MessageSource messageSource;
    @MockBean
    private GeneInfoService geneInfoService;
    @MockBean
    private PrivacyService privacyService;
    @MockBean
    private SecureRandom secureRandom;
    @MockBean
    private OntologyService ontologyService;
    @MockBean
    private SecureTokenChallenge<HttpServletRequest> secureTokenChallenge;
    @MockBean
    private TaxonService taxonService;


    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private GOService goService;

    private Taxon taxon = new Taxon();

    @Test
    public void test() {
        taxon.setId( 9606 );
        when( taxonService.findByActiveTrue() ).thenReturn( Collections.singleton( taxon ) );
        goService.updateGoTerms();
        User user = new User();
        user.getUserGenes().put( 1, createGene( "BRCA1", 672 ) );
        user.getUserGenes().put( 2, createGene( "BRCA2", 675 ) );
        assertThat( userService.recommendTerms( user, taxon, null ) )
                .extracting( GeneOntologyTerm::getGoId )
                .containsExactlyInAnyOrder( "GO:0000800", "GO:0006978" );
    }

    private UserGene createGene( String symbol, int geneId ) {
        UserGene gene = new UserGene();
        gene.setSymbol( symbol );
        gene.setGeneId( geneId );
        gene.setTier( TierType.TIER1 );
        gene.setTaxon( taxon );
        return gene;
    }
}
