package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.AccessToken;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Integer> {

    AccessToken findByToken( String token );
}
