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
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@Import(JpaAuditingConfig.class)
public class VerificationTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    private VerificationToken validToken;
    private VerificationToken expiredToken;

    @Before
    public void setUp() {
        // given
        User user = entityManager.persist( createUnpersistedUser() );

        validToken = new VerificationToken();
        validToken.updateToken( "validtoken" );
        validToken.setUser( user );
        validToken.setEmail( user.getEmail() );
        validToken = entityManager.persistAndFlush( validToken );
        assertThat( validToken.getCreatedAt() )
                .isBetween( Instant.now().minus( 500, ChronoUnit.MILLIS ), Instant.now() );

        User user2 = entityManager.persistAndFlush( createUnpersistedUser() );

        expiredToken = new VerificationToken();
        expiredToken.setToken( "expiredtoken" );
        expiredToken.setUser( user2 );
        expiredToken.setEmail( user2.getEmail() );
        expiredToken.setExpiryDate( Instant.now() );
        expiredToken = entityManager.persistAndFlush( expiredToken );
        assertThat( expiredToken.getCreatedAt() )
                .isBetween( Instant.now().minus( 500, ChronoUnit.MILLIS ), Instant.now() );
    }

    @Test
    public void findByToken_whenValidToken_thenReturnToken() {
        assertThat( verificationTokenRepository.findByToken( "validtoken" ) ).hasValue( validToken );
    }

    @Test
    public void findByToken_whenExpredToken_thenReturnToken() {
        assertThat( verificationTokenRepository.findByToken( "expiredtoken" ) ).hasValue( expiredToken );
    }

    @Test
    public void findByToken_whenInvalidToken_thenReturnNull() {
        assertThat( verificationTokenRepository.findByToken( "invalidtoken" ) ).isEmpty();
    }

    @Test
    public void deleteAllExpiredSince_whenValidDate_thenDeleteTokens() {
        verificationTokenRepository.deleteAllExpiredSince( Instant.now() );
        assertThat( verificationTokenRepository.findAll() ).containsExactly( validToken );

    }

}
