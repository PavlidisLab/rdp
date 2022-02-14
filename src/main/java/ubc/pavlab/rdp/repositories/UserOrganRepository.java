package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.UserOrgan;

import java.util.Collection;

@Repository
public interface UserOrganRepository extends JpaRepository<UserOrgan, Integer> {

    Collection<UserOrgan> findByDescriptionContainingIgnoreCase( String description );
}
