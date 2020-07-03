package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.TierService;
import ubc.pavlab.rdp.util.BaseTest;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserGeneRepositoryTest extends BaseTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserGeneRepository userGeneRepository;

    @Autowired
    TierService tierService;

    private User user;
    private Taxon taxon;

    @Before
    public void setUp() {
        // given
        taxon = entityManager.persist( createTaxon( 1 ) );
        user = entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();
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

        int count = userGeneRepository.countByTierIn( tierService.getEnabledTiers());

        assertThat( count ).isEqualTo( 0 );
    }

    @Test
    public void countByTierIn_whenMultipleMatches_thenCountAll() {

        UserGene ug = UserGene.createUserGeneFromGene( createGene( 99, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        user.getUserGenes().put( ug.getGeneId(), ug );
        entityManager.persist( user );
        entityManager.flush();

        int count = userGeneRepository.countByTierIn( EnumSet.of ( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countByTierIn_whenMultipleMatchesInDifferentUsers_thenCountAll() {

        entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();

        int count = userGeneRepository.countByTierIn( EnumSet.of ( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierExactMatch_thenCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( EnumSet.of( TierType.TIER1) );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierManualMatch_thenCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenTierNotMatch_thenDontCount() {

        int count = userGeneRepository.countDistinctGeneByTierIn( tierService.getEnabledTiers());

        assertThat( count ).isEqualTo( 0 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenMultipleMatches_thenCountDistinct() {

        UserGene ug = UserGene.createUserGeneFromGene( createGene( 99, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        user.getUserGenes().put( ug.getGeneId(), ug );
        entityManager.persist( user );
        entityManager.flush();

        int count = userGeneRepository.countDistinctGeneByTierIn( Collections.singleton( TierType.TIER1 ) );

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctGeneByTierIn_whenMultipleMatchesInDifferentUsers_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();

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
        entityManager.flush();

        int count = userGeneRepository.countDistinctUserByTaxon( taxon );

        assertThat( count ).isEqualTo( 1 );
    }

    @Test
    public void countDistinctUserByTaxon_whenMultipleMatchesInDifferentUsers_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();

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
        entityManager.flush();

        int count = userGeneRepository.countDistinctUser();

        assertThat( count ).isEqualTo( 2 );
    }

    @Test
    public void countDistinctUser_whenMultipleMatchesDifferentTaxon_thenCountDistinct() {

        // Create another user.
        entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();

        // Create another user.
        Taxon taxon2 = entityManager.persist( createTaxon( 2 ) );
        entityManager.persist( createUserWithGenes( taxon2 ) );
        entityManager.flush();

        int count = userGeneRepository.countDistinctUser();

        assertThat( count ).isEqualTo( 3 );
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
        entityManager.flush();

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
        entityManager.flush();

        Collection<UserGene> ugs = userGeneRepository.findByGeneIdAndTierIn( 1, TierType.MANUAL );

        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ),  user2.getUserGenes().get( 1 ) );
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
        entityManager.flush();

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
        entityManager.flush();

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
        entityManager.flush();

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
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ),  user.getUserGenes().get( 3 ) );
    }

    @Test
    public void findBySymbolContainingIgnoreCaseAndTaxonAndTierIn_whenMultipleUsers_ReturnUserGenes() {

        // Create another user.
        User user2 = entityManager.persist( createUserWithGenes( taxon ) );
        entityManager.flush();

        Collection<UserGene> ugs = userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( "Gene", taxon, TierType.MANUAL );
        assertThat( ugs ).containsExactly( user.getUserGenes().get( 1 ), user.getUserGenes().get( 2 ), user2.getUserGenes().get( 1 ), user2.getUserGenes().get( 2 ) );
    }

}
