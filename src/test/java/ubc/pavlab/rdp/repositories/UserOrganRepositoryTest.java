package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserOrgan;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

@RunWith(SpringRunner.class)
@DataJpaTest
public class UserOrganRepositoryTest {

    @Autowired
    TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOrganRepository userOrganRepository;

    private UserOrgan createUserOrgan( User user, String uberonId, String name, String description ) {
        UserOrgan userOrgan = new UserOrgan();
        userOrgan.setUser( user );
        userOrgan.setUberonId( uberonId );
        userOrgan.setName( name );
        userOrgan.setDescription( description );
        return userOrgan;
    }

    private UserOrgan userOrgan;

    @Before
    public void setUp() {
        User user = entityManager.persistAndFlush( createUnpersistedUser() );
        userOrgan = entityManager.persistAndFlush( createUserOrgan( user, "UBERON_....", "Limb/Appendage", "Limb or appendage" ) );
    }

    @Test
    public void findByDescriptionContainingIgnoreCase() {
        Collection<UserOrgan> userOrgans = userOrganRepository.findByDescriptionContainingIgnoreCase( "appendage" );
        assertThat( userOrgans ).containsExactly( userOrgan );
    }
}
