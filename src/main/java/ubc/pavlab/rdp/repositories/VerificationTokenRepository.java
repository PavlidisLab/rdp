package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.VerificationToken;

import java.time.Instant;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {
    VerificationToken findByToken( String token );

    @Modifying
    @Query("delete from VerificationToken t where t.expiryDate <= :since")
    void deleteAllExpiredSince( Instant since );
}
