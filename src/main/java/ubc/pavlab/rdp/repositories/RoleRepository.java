package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Role;

/**
 * Created by mjacobson on 16/01/18.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    @Nullable
    Role findByRole( String role );

}