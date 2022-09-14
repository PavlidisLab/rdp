package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.JpaAuditingConfig;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Import(JpaAuditingConfig.class)
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void findByEmail_whenMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        User found = userRepository.findByEmailIgnoreCase( user.getEmail() );

        // then
        assertThat( found.getEmail() )
                .isEqualTo( user.getEmail() );
    }

    @Test
    public void findByEmail_whenCaseInsensitiveMatch_thenReturnUser() {

        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        User found = userRepository.findByEmailIgnoreCase( user.getEmail().toUpperCase() );

        // then
        assertThat( found ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameNoMatch_thenReturnEmptyCollection() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = "xyz";
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).isEmpty();
    }

    @Test
    public void findByNameLike_whenMatchBothNameAndLastName_thenReturnSingleUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setName( "Name" );
        user.getProfile().setLastName( "Name" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = "Name";
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenCaseInsensitiveMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getName().toUpperCase();
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameFirstNameExactMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getName();
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameFirstNameStartsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getName().substring( 0, 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameFirstNameEndsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getName().substring( 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameFirstNameContainsMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getName().substring( 1, 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameLastNameExactMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getLastName();
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameLastNameStartsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getLastName().substring( 0, 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameLastNameEndsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getLastName().substring( 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenNameLastNameContainsMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getLastName().substring( 1, 3 );
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByNameLike_whenMultipleMatches_thenReturnMultipleUsers() {
        // given
        User user = createUnpersistedUser();
        User user2 = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.persist( user2 );
        entityManager.flush();

        // when
        String search = user.getProfile().getLastName();
        Collection<User> found = userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 2 );
        assertThat( found ).containsExactly( user, user2 );
    }


    @Test
    public void findByDescriptionLike_whenNoMatch_thenReturnEmptyCollection() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = "xyz";
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).isEmpty();
    }

    @Test
    public void findByDescriptionLike_whenMultipleMatchInSameUser_thenReturnSingleUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );
        Taxon taxon = entityManager.persist( createTaxon( 1 ) );
        user.getTaxonDescriptions().put( taxon, "Taxon Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = "Description";
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenCaseInsensitiveMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getDescription().toUpperCase();
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenDescriptionStartsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getDescription().substring( 0, 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenDescriptionEndsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getDescription().substring( 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenDescriptionContainsMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getProfile().getDescription().substring( 1, 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenTaxonDescriptionStartsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        Taxon taxon = entityManager.persist( createTaxon( 1 ) );
        user.getTaxonDescriptions().put( taxon, "Taxon Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getTaxonDescriptions().get( taxon ).substring( 0, 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenTaxonDescriptionEndsWithMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        Taxon taxon = entityManager.persist( createTaxon( 1 ) );
        user.getTaxonDescriptions().put( taxon, "Taxon Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getTaxonDescriptions().get( taxon ).substring( 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenTaxonDescriptionContainsMatch_thenReturnUser() {
        // given
        User user = createUnpersistedUser();
        Taxon taxon = entityManager.persist( createTaxon( 1 ) );
        user.getTaxonDescriptions().put( taxon, "Taxon Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = user.getTaxonDescriptions().get( taxon ).substring( 1, 3 );
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void findByDescriptionLike_whenMultipleMatches_thenReturnMultipleUsers() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );
        User user2 = createUnpersistedUser();
        user2.getProfile().setDescription( "Test Description" );

        entityManager.persist( user );
        entityManager.persist( user2 );
        entityManager.flush();

        // when
        String search = user.getProfile().getDescription();
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 2 );
        assertThat( found ).containsExactly( user, user2 );
    }

    @Test
    public void findByDescriptionLike_whenMultipleMatchInMultipleTaxa_thenReturnSingleUser() {
        // given
        User user = createUnpersistedUser();
        user.getProfile().setDescription( "Test Description" );

        Taxon taxon = entityManager.persist( createTaxon( 1 ) );
        user.getTaxonDescriptions().put( taxon, "Taxon Description" );

        Taxon taxon2 = entityManager.persist( createTaxon( 2 ) );
        user.getTaxonDescriptions().put( taxon2, "Taxon Description" );

        entityManager.persist( user );
        entityManager.flush();

        // when
        String search = "Description";
        Collection<User> found = userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + search + "%" );

        // then
        assertThat( found ).hasSize( 1 );
        assertThat( found.iterator().next() ).isEqualTo( user );
    }

    @Test
    public void save_whenUserWithCompleteProfile_thenSucceed() {
        User user = createUnpersistedUser();
        user.getProfile().setResearcherPosition( ResearcherPosition.PRINCIPAL_INVESTIGATOR );
        user.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        User persistedUser = entityManager.persistAndFlush( user );
        assertThat( persistedUser.getCreatedAt() ).isCloseTo( Instant.now(), 500 );
        assertThat( persistedUser.getProfile() )
                .hasFieldOrPropertyWithValue( "researcherPosition", ResearcherPosition.PRINCIPAL_INVESTIGATOR )
                .hasFieldOrPropertyWithValue( "researcherCategories", EnumSet.of( ResearcherCategory.IN_SILICO ) );
    }

    @Test
    public void save_whenUserHasMultipleResearcherCategories_thenSucceed() {
        User user = createUnpersistedUser();
        user.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        user.getProfile().getResearcherCategories().add( ResearcherCategory.IN_VIVO );
        user = entityManager.persistAndFlush( user );
        assertThat( user.getProfile().getResearcherCategories() )
                .containsExactlyInAnyOrder( ResearcherCategory.IN_SILICO, ResearcherCategory.IN_VIVO );
    }

    @Test
    public void delete_whenVerificationToken_thenSucceed() {
        User user = createUnpersistedUser();
        user = entityManager.persistAndFlush( user );

        VerificationToken token = entityManager.persistAndFlush( createVerificationToken( user, "token123" ) );
        Taxon humanTaxon = entityManager.find( Taxon.class, 9606 );
        Gene gene = createGene( 1, humanTaxon );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        entityManager.persistAndFlush( user );

        entityManager.refresh( user );

        assertThat( user.getVerificationTokens() ).contains( token );
        assertThat( user.getUserGenes().values() ).contains( userGene );

        userRepository.delete( user );
        userRepository.flush();
    }

    @Autowired
    private VerificationTokenRepository accessTokenRepository;

    @Test
    public void delete_whenUserHasMultipleAssociations_thenSucceed() {
        User user = createUnpersistedUser();
        user = entityManager.persist( user );

        VerificationToken token = entityManager.persistAndFlush( createVerificationToken( user, "token123" ) );
        Taxon humanTaxon = entityManager.find( Taxon.class, 9606 );
        Gene gene = createGene( 1, humanTaxon );
        UserGene userGene = entityManager.persist( createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );
        OrganInfo organInfo = new OrganInfo();
        organInfo.setUberonId( "UBERON:00001" );
        UserOrgan userOrgan = entityManager.persistAndFlush( createUserOrgan( user, organInfo ) );

        user.getVerificationTokens().add( token );
        user.getRoles().add( roleRepository.findByRole( "ROLE_USER" ) );
        user.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        user.getUserOrgans().put( userOrgan.getUberonId(), userOrgan );
        user.getTaxonDescriptions().put( humanTaxon, "I'm a human researcher." );
        entityManager.persistAndFlush( user );

        entityManager.refresh( user );
        assertThat( user.getVerificationTokens() ).contains( token );
        assertThat( user.getUserGenes().values() ).contains( userGene );
        assertThat( user.getUserOrgans().values() ).contains( userOrgan );

        userRepository.delete( user );
        userRepository.flush();

        // make sure that the token is not lingering
        assertThat( accessTokenRepository.existsById( token.getId() ) ).isFalse();
    }
}
