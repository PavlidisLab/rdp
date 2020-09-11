package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

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
        Collection<User> found = userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( search, search );

        // then
        assertThat( found ).hasSize( 2 );
        assertThat( found ).containsExactly( user, user2 );
    }
}
