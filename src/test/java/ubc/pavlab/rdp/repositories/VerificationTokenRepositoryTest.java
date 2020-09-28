package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.VerificationToken;

import java.util.Date;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createUnpersistedUser;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

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
        entityManager.persist( validToken );

        user2 = entityManager.persist( createUnpersistedUser() );

        expiredToken = new VerificationToken();
        expiredToken.setToken( "expiredtoken" );
        expiredToken.setUser( user2 );
        expiredToken.setExpiryDate( new Date() );
        entityManager.persist( expiredToken );

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
    public void findByUser_whenValidUser_thenReturnToken() {

        VerificationToken found = verificationTokenRepository.findByUser( user );
        assertThat( found ).isEqualTo( validToken );
    }

    @Test
    public void findByUser_whenValidUserHasOnlyExpiredToken_thenReturnToken() {

        VerificationToken found = verificationTokenRepository.findByUser( user2 );
        assertThat( found ).isEqualTo( expiredToken );
    }

    @Test
    public void findByUser_whenValidUserHasMultipleTokens_thenError() {

        VerificationToken validToken2 = new VerificationToken();
        validToken2.updateToken( "validtoken2" );
        validToken2.setUser( user );
        entityManager.persist( validToken2 );
        entityManager.flush();

        try {
            VerificationToken found = verificationTokenRepository.findByUser( user );
        } catch ( IncorrectResultSizeDataAccessException e ) {
            // Expected
            return;
        }
        fail( "Should have thrown IncorrectResultSizeDataAccessException" );
    }

    @Test
    public void findByUser_whenInvalidUser_thenReturnNull() {

        VerificationToken found = verificationTokenRepository.findByUser( createUser( -1 ) );
        assertThat( found ).isNull();
    }

    @Test
    public void deleteAllExpiredSince_whenValidDate_thenDeleteTokens() {
        verificationTokenRepository.deleteAllExpiredSince( new Date() );
        assertThat( verificationTokenRepository.findAll() ).containsExactly( validToken );

    }

}
