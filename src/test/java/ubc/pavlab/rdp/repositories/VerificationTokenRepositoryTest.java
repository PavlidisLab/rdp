package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
public class VerificationTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    private User user;
    private User user2;
    private VerificationToken validToken;
    private VerificationToken expiredToken;

    @Before
    public void setUp() {
        // given
        user = entityManager.persist( createUnpersistedUser() );

        validToken = new VerificationToken();
        validToken.updateToken( "validtoken" );
        validToken.setUser( user );
        validToken.setEmail( user.getEmail() );
        entityManager.persist( validToken );

        user2 = entityManager.persistAndFlush( createUnpersistedUser() );

        expiredToken = new VerificationToken();
        expiredToken.setToken( "expiredtoken" );
        expiredToken.setUser( user2 );
        expiredToken.setEmail( user2.getEmail() );
        expiredToken.setExpiryDate( Timestamp.from( Instant.now() ) );
        expiredToken = entityManager.persistAndFlush( expiredToken );

        entityManager.flush();
    }

    @Test
    public void findByToken_whenValidToken_thenReturnToken() {

        VerificationToken found = verificationTokenRepository.findByToken( "validtoken" );
        assertThat( found ).isEqualTo( validToken );
    }

    @Test
    public void findByToken_whenExpredToken_thenReturnToken() {

        VerificationToken found = verificationTokenRepository.findByToken( "expiredtoken" );
        assertThat( found ).isEqualTo( expiredToken );
    }

    @Test
    public void findByToken_whenInvalidToken_thenReturnNull() {

        VerificationToken found = verificationTokenRepository.findByToken( "invalidtoken" );
        assertThat( found ).isNull();
    }

    @Test
    public void deleteAllExpiredSince_whenValidDate_thenDeleteTokens() {
        verificationTokenRepository.deleteAllExpiredSince( Timestamp.from( Instant.now() ) );
        assertThat( verificationTokenRepository.findAll() ).containsExactly( validToken );

    }

}
