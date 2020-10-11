package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.sql.Timestamp;
import java.util.Date;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {
    VerificationToken findByToken( String token );

    VerificationToken findByUser( User user );

    @Modifying
    @Query("delete from VerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince( Timestamp now );
}
