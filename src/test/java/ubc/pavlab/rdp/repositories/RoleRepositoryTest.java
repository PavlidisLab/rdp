package ubc.pavlab.rdp.repositories;

import org.hibernate.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Role;

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

    /**
     * This is not recommended usage, but we use the {@link org.hibernate.annotations.NaturalId} to mark columns in
     * order to correctly select those for equals() and hashCode implementation.
     */
    @Test
    public void findByNaturalId() {
        Role userRole = entityManager.getEntityManager().unwrap( Session.class )
                .bySimpleNaturalId( Role.class )
                .load( "ROLE_USER" );
        assertThat( userRole )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "role", "ROLE_USER" );
    }

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

    @Test
    public void findByRole_whenExpected_thenReturnRole() {
        assertThat( roleRepository.findByRole( "ROLE_USER" ) ).isNotNull();
        assertThat( roleRepository.findByRole( "ROLE_ADMIN" ) ).isNotNull();
        assertThat( roleRepository.findByRole( "ROLE_SERVICE_ACCOUNT" ) ).isNotNull();
    }

}
