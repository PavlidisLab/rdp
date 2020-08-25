package ubc.pavlab.rdp.repositories;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void findByRole_whenValidRole_thenReturnRole() {

        Role role = new Role();
        role.setRole( "TEST_ROLE" );

        entityManager.persist( role );
        entityManager.flush();

        Role found = roleRepository.findByRole( "TEST_ROLE" );

        assertThat( found ).isEqualTo( role );

    }


    @Test
    public void findByRole_whenInvalidRole_thenReturnNull() {

        Role found = roleRepository.findByRole( "XXX" );

        assertThat( found ).isNull();

    }

}
