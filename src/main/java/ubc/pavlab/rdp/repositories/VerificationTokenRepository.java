package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.util.Date;
import java.util.stream.Stream;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Integer> {
    VerificationToken findByToken( String token );
    VerificationToken findByUser( User user );
    Stream<VerificationToken> findAllByExpiryDateLessThan( Date now );
    void deleteByExpiryDateLessThan( Date now );

    @Modifying
    @Query("delete from VerificationToken t where t.expiryDate <= ?1")
    void deleteAllExpiredSince( Date now );
}
