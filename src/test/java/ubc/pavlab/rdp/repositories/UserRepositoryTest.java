package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.util.BaseTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class UserRepositoryTest extends BaseTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void whenFindByEmail_thenReturnUser() {
        // given
        User user = createUnpersistedUser();

        entityManager.persist( user );
        entityManager.flush();

        // when
        User found = userRepository.findByEmail( user.getEmail() );

        // then
        assertThat( found.getEmail() )
                .isEqualTo( user.getEmail() );
    }
}
