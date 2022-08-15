package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.JpaAuditingConfig;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Import(JpaAuditingConfig.class)
public class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;

    @Before
    public void setUp() {
        // given
        User user = entityManager.persist( createUnpersistedUser() );

        validToken = new PasswordResetToken();
        validToken.updateToken( "validtoken" );
        validToken.setUser( user );
        entityManager.persist( validToken );
        assertThat( validToken.getCreatedAt() ).isCloseTo( Instant.now(), 500 );

        User user2 = entityManager.persistAndFlush( createUnpersistedUser() );

        expiredToken = new PasswordResetToken();
        expiredToken.setToken( "expiredtoken" );
        expiredToken.setUser( user2 );
        expiredToken.setExpiryDate( Timestamp.from( Instant.now() ) );
        expiredToken = entityManager.persistAndFlush( expiredToken );
        assertThat( expiredToken.getCreatedAt() ).isCloseTo( Instant.now(), 500 );
    }

    @Test
    public void findByToken_whenValidToken_thenReturnToken() {

        PasswordResetToken found = passwordResetTokenRepository.findByToken( "validtoken" );
        assertThat( found ).isEqualTo( validToken );
    }

    @Test
    public void findByToken_whenExpredToken_thenReturnToken() {

        PasswordResetToken found = passwordResetTokenRepository.findByToken( "expiredtoken" );
        assertThat( found ).isEqualTo( expiredToken );
    }

    @Test
    public void findByToken_whenInvalidToken_thenReturnNull() {

        PasswordResetToken found = passwordResetTokenRepository.findByToken( "invalidtoken" );
        assertThat( found ).isNull();
    }

    @Test
    public void deleteAllExpiredSince_whenValidDate_thenDeleteTokens() {
        passwordResetTokenRepository.deleteAllExpiredSince( new Date() );
        assertThat( passwordResetTokenRepository.findAll() ).containsExactly( validToken );
    }

}
