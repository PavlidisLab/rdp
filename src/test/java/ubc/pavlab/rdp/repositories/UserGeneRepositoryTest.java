package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.persistence.EntityManager;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserGeneRepositoryTest {

    @TestConfiguration
    static class UserGeneRepositoryTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            return new ApplicationSettings();
        }

        @Bean
        public PrivacyService privacyService() {
            return new PrivacyServiceImpl();
        }

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }

        @Bean
        public UserGeneService userGeneService() {
            return new UserGeneServiceImpl();
        }

        @Bean
        public SecureRandom secureRandom() throws NoSuchAlgorithmException {
            return SecureRandom.getInstance( "SHA1PRNG" );
        }

        @Bean
        public OntologyService ontologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository ) {
            return new OntologyService( ontologyRepository, ontologyTermInfoRepository );
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserGeneRepository userGeneRepository;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private TierService tierService;

    @MockBean
    private GOService goService;

    @MockBean
    private OrganInfoService organInfoService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private GeneInfoService geneinfoService;

    private User user;
    private Taxon taxon;

    @Before
    public void setUp() {
        taxon = entityManager.persistAndFlush( createTaxon( 1 ) );
        user = entityManager.persistAndFlush( createUserWithGenes( taxon ) );
    }

    private User createUserWithGenes( Taxon taxon ) {
        User user = createUnpersistedUser();
        UserGene ug = UserGene.createUserGeneFromGene( createGene( 1, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        ug.setSymbol( "Gene1" );
        user.getUserGenes().put( ug.getGeneId(), ug );

        ug = UserGene.createUserGeneFromGene( createGene( 2, taxon ), user, TierType.TIER2, PrivacyLevelType.PRIVATE );
        ug.setSymbol( "Gene2" );
        user.getUserGenes().put( ug.getGeneId(), ug );

        ug = UserGene.createUserGeneFromGene( createGene( 3, taxon ), user, TierType.TIER3, PrivacyLevelType.PRIVATE );
        ug.setSymbol( "Gene3" );
        user.getUserGenes().put( ug.getGeneId(), ug );

        return user;
    }

    @Test
    public void countByTierIn_whenTierExactMatch_thenCount() {

        int count = userGeneRepository.countByTierIn( Collections.singleton( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countByTierIn_whenTierManualMatch_thenCount() {

        int count = userGeneRepository.countByTierIn( TierType.MANUAL );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countByTierIn_whenTierNotMatch_thenDontCount() {

        int count = userGeneRepository.countByTierIn( tierService.getEnabledTiers() );

        assertThat( count ).isEqualTo( 0 );
    }

    @Test
    public void countByTierIn_whenMultipleMatches_thenCountAll() {

        UserGene ug = UserGene.createUserGeneFromGene( createGene( 99, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        user.getUserGenes().put( ug.getGeneId(), ug );
        user = entityManager.persistAndFlush( user );

        int count = userGeneRepository.countByTierIn( EnumSet.of( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countByTierIn_whenMultipleMatchesInDifferentUsers_thenCountAll() {

        entityManager.persist( createUserWithGenes( taxon ) );

        int count = userGeneRepository.countByTierIn( EnumSet.of( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierExactMatch_thenCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( EnumSet.of( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierManualMatch_thenCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierNotMatch_thenDontCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( tierService.getEnabledTiers() );

        assertThat( count ).isEqualTo( 0 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenMultipleMatches_thenCountDistinct() {

        UserGene ug = UserGene.createUserGeneFromGene( createGene( 99, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        user.getUserGenes().put( ug.getGeneId(), ug );
        entityManager.persist( user );

        int count = userGeneRepository.countDistinctGeneByTierIn( Collections.singleton( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenMultipleMatchesInDifferentUsers_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );

        int count = userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctUserByTaxon_whenTaxonExactMatch_thenCount() {

        int count = userGeneRepository.countDistinctUserByTaxon( taxon );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctUserByTaxon_whenTaxonNotMatch_thenDontCount() {

        int count = userGeneRepository.countDistinctUserByTaxon( createTaxon( 2 ) );

        assertThat( count ).isEqualTo( 0 );
    }

    @Test
    public void countDistinctUserByTaxon_whenMultipleMatches_thenCountDistinct() {

        UserGene ug = UserGene.createUserGeneFromGene( createGene( 99, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        user.getUserGenes().put( ug.getGeneId(), ug );
        entityManager.persist( user );

        int count = userGeneRepository.countDistinctUserByTaxon( taxon );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctUserByTaxon_whenMultipleMatchesInDifferentUsers_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );

        int count = userGeneRepository.countDistinctUserByTaxon( taxon );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctUser_thenCount() {

        int count = userGeneRepository.countDistinctUser();

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctUser_whenMultipleMatches_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );

        int count = userGeneRepository.countDistinctUser();

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctUser_whenMultipleMatchesDifferentTaxon_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );

        // Create another user.
        Taxon taxon2 = entityManager.persist( createTaxon( 2 ) );
        entityManager.persist( createUserWithGenes( taxon2 ) );

        int count = userGeneRepository.countDistinctUser();

        assertThat( count ).isEqualTo( 3 );
    }

    @Test
    public void findByUserEnabledTrue() {
        Gene gene = entityManager.persist( createGene( 1, taxon ) );
        user.setEnabled( true );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PUBLIC );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );
        assertThat( userGeneRepository.findByUserEnabledTrue( null ) )
                .contains( userGene );
    }

    @Test
    public void findByUserEnabledTrue_whenUserIsNotEnabled_thenExcludeTheGene() {
        Gene gene = entityManager.persist( createGene( 1, taxon ) );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PUBLIC );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );
        assertThat( user.isEnabled() ).isFalse();
        assertThat( userGeneRepository.findByUserEnabledTrue( null ) )
                .doesNotContain( userGene );
    }

    @Test
    public void findAllByPrivacyLevelAndUserProfilePrivacyLevel() {
        Gene gene1 = entityManager.persist( createGene( 1, taxon ) );
        Gene gene2 = entityManager.persist( createGene( 2, taxon ) );
        Gene gene3 = entityManager.persist( createGene( 3, taxon ) );
        user.setEnabled( true );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PUBLIC );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene1, user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );
        UserGene sharedUserGene = entityManager.persist( createUnpersistedUserGene( gene2, user, TierType.TIER1, PrivacyLevelType.SHARED ) );
        UserGene privateUserGene = entityManager.persist( createUnpersistedUserGene( gene3, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PUBLIC, null ) )
                .containsExactly( userGene );
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.SHARED, null ) )
                .containsExactly( sharedUserGene );
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PRIVATE, null ) )
                .containsExactly( privateUserGene );
    }

    @Test
    public void findAllByPrivacyLevelAndUserProfilePrivacyLevel_whenProfileIsPrivate() {
        Gene gene1 = entityManager.persist( createGene( 1, taxon ) );
        Gene gene2 = entityManager.persist( createGene( 2, taxon ) );
        Gene gene3 = entityManager.persist( createGene( 3, taxon ) );
        user.setEnabled( true );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PRIVATE );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene1, user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );
        UserGene sharedUserGene = entityManager.persist( createUnpersistedUserGene( gene2, user, TierType.TIER1, PrivacyLevelType.SHARED ) );
        UserGene privateUserGene = entityManager.persist( createUnpersistedUserGene( gene3, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PUBLIC, null ) ).isEmpty();
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.SHARED, null ) ).isEmpty();
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PRIVATE, null ) )
                .containsExactly( userGene, sharedUserGene, privateUserGene );
    }

    @Test
    public void findAllByPrivacyLevelAndUserProfilePrivacyLevel_whenGenePrivacyLevelIsNull_thenFallbackOnProfile() {
        Gene gene = entityManager.persist( createGene( 1, taxon ) );
        user.setEnabled( true );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PUBLIC );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, null ) );
        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PUBLIC, null ) )
                .containsExactly( userGene );
    }

    @Test
    public void findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel_whenUserIsNotEnabled_thenReturnNothing() {
        Gene gene = entityManager.persist( createGene( 1, taxon ) );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PUBLIC );
        user.getUserGenes().clear();
        user = entityManager.persistAndFlush( user );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );

        // ensure that the gene would be public if it were not for the enabled status of the user
        assertThat( user.isEnabled() ).isFalse();
        assertThat( user.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );
        assertThat( userGene.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );

        assertThat( userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( PrivacyLevelType.PUBLIC, null ) )
                .doesNotContain( userGene );
    }

    @Test
    public void findByGeneId_whenValidId_ReturnUserGene() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneId( 1 );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
        assertThat( ugs.iterator().next().getTier() ).isEqualTo( user.getUserGenes().get( 1 ).getTier() );
    }

    @Test
    public void findByGeneId_whenInvalidId_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneId( -1 );

        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findByGeneIdAndTier_whenValidIdAndTierMatch_ReturnUserGene() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTier( 1, TierType.TIER1 );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
        assertThat( ugs.iterator().next().getTier() ).isEqualTo( user.getUserGenes().get( 1 ).getTier() );

    }

    @Test
    public void findByGeneIdAndTier_whenInvalidId_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTier( -1, TierType.TIER1 );

        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findByGeneIdAndTier_whenTierNotMatch_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTier( 1, TierType.TIER2 );

        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findByGeneIdAndTier_whenValidIdAndMultipleMatch_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );

        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTier( 1, TierType.TIER1 );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user2.getUserGenes().get( 1 ) );
    }

    @Test
    public void findByGeneIdAndTierIn_whenValidIdAndTierMatch_ReturnUserGene() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( 1, Collections.singleton( TierType.TIER1 ) );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
        assertThat( ugs.iterator().next().getTier() ).isEqualTo( user.getUserGenes().get( 1 ).getTier() );
    }

    @Test
    public void findByGeneIdAndTierIn_whenValidIdAndTierManualMatch_ReturnUserGene() {

        User user2 = createUnpersistedUser();
        UserGene ug = UserGene.createUserGeneFromGene( createGene( 1, taxon ), user2, TierType.TIER2, PrivacyLevelType.PRIVATE );
        user2.getUserGenes().put( ug.getGeneId(), ug );
        entityManager.persist( user2 );

        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( 1, TierType.MANUAL );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user2.getUserGenes().get( 1 ) );
    }

    @Test
    public void findByGeneIdAndTierIn_whenInvalidId_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( -1, Collections.singleton( TierType.TIER1 ) );

        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findByGeneIdAndTierIn_whenTierNotMatch_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( 1, Collections.singleton( TierType.TIER2 ) );

        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findByGeneIdAndTierIn_whenValidIdAndMultipleMatch_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );

        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( 1, Collections.singleton( TierType.TIER1 ) );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user2.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenValidSymbolAndTaxon_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "Gene1", taxon );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenValidSymbolCaseInsensitive_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "gEnE1", taxon );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenInvalidSymbol_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "GeneX", taxon );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenInvalidTaxon_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "Gene1", createTaxon( 2 ) );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenSymbolStartsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "Gene", taxon );
        assertThat( ugs ).containsExactlyElementsOf( user.getUserGenes().values() );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenSymbolEndsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "3", taxon );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 3 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenSymbolContains_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "ne", taxon );
        assertThat( ugs ).containsExactlyElementsOf( user.getUserGenes().values() );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxon_whenMultipleUsers_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );

        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( "Gene1", taxon );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user2.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenValidSymbolAndTaxonAndTier_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "Gene1", taxon, TierType.TIER1 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenValidSymbolCaseInsensitive_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "gEnE1", taxon, TierType.TIER1 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenInvalidSymbol_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "GeneX", taxon, TierType.TIER1 );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenInvalidTaxon_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "Gene1", createTaxon( 2 ), TierType.TIER1 );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenInvalidTier_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "Gene1", taxon, TierType.TIER2 );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenSymbolStartsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "Gene", taxon, TierType.TIER2 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 2 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenSymbolEndsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "3", taxon, TierType.TIER3 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 3 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenSymbolContains_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "ne", taxon, TierType.TIER1 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTier_whenMultipleUsers_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );

        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( "Gene1", taxon, TierType.TIER1 );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user2.getUserGenes().get( 1 ) );
    }

//    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTierIn(String symbolContaining, Taxon taxon, Set<TierType> tiers);

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenValidSymbolAndTaxonAndTier_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene1", taxon, Collections.singleton( TierType.TIER1 ) );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenValidSymbolAndTaxonAndManualTier_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene", taxon, TierType.MANUAL );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user.getUserGenes().get( 2 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenValidSymbolCaseInsensitive_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "gEnE1", taxon, Collections.singleton( TierType.TIER1 ) );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenInvalidSymbol_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "GeneX", taxon, Collections.singleton( TierType.TIER1 ) );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenInvalidTaxon_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene1", createTaxon( 2 ), Collections.singleton( TierType.TIER1 ) );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenInvalidTier_ReturnEmptyCollection() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene1", taxon, Collections.singleton( TierType.TIER2 ) );
        assertThat( ugs ).isEmpty();
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenSymbolStartsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene", taxon, Collections.singleton( TierType.TIER2 ) );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 2 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenSymbolEndsWith_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "3", taxon, Collections.singleton( TierType.TIER3 ) );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 3 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenSymbolContains_ReturnUserGenes() {
        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "ne", taxon, Sets.newSet( TierType.TIER1, TierType.TIER3 ) );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user.getUserGenes().get( 3 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenMultipleUsers_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );

        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene", taxon, TierType.MANUAL );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user.getUserGenes().get( 2 ), user2.getUserGenes().get( 1 ), user2.getUserGenes().get( 2 ) );
    }

    @Test
    public void findById_whenUserGenePrivacyLevelIsHigher_ReturnUserPrivacyLevel() {
        UserGene userGene = user.getUserGenes().get( 1 );
        userGene.getUser().getProfile().setPrivacyLevel( PrivacyLevelType.SHARED );
        userGene.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        assertThat( userGene.getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );
        assertThat( userGene.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.SHARED );
    }
}
