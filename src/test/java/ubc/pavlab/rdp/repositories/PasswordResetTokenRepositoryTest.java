package ubc.pavlab.rdp.repositories;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.DatabaseMigrationConfig;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.User;

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
@Import(DatabaseMigrationConfig.class)
public class PasswordResetTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User user;
    private User user2;
    private PasswordResetToken validToken;
    private PasswordResetToken expiredToken;

    @Before
    public void setUp() {
        // given
        user = entityManager.persist( createUnpersistedUser() );

        validToken = new PasswordResetToken();
        validToken.updateToken( "validtoken" );
        validToken.setUser( user );
        entityManager.persist( validToken );

        user2 = entityManager.persist( createUnpersistedUser() );

        expiredToken = new PasswordResetToken();
        expiredToken.setToken( "expiredtoken" );
        expiredToken.setUser( user2 );
        expiredToken.setExpiryDate( new Date() );
        entityManager.persist( expiredToken );

        entityManager.flush();
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
    public void findByUser_whenValidUser_thenReturnToken() {

        PasswordResetToken found = passwordResetTokenRepository.findByUser( user );
        assertThat( found ).isEqualTo( validToken );
    }

    @Test
    public void findByUser_whenValidUserHasOnlyExpiredToken_thenReturnToken() {

        PasswordResetToken found = passwordResetTokenRepository.findByUser( user2 );
        assertThat( found ).isEqualTo( expiredToken );
    }

    @Test
    public void findByUser_whenValidUserHasMultipleTokens_thenError() {

        PasswordResetToken validToken2 = new PasswordResetToken();
        validToken2.updateToken( "validtoken2" );
        validToken2.setUser( user );
        entityManager.persist( validToken2 );
        entityManager.flush();

        try {
            PasswordResetToken found = passwordResetTokenRepository.findByUser( user );
        } catch (IncorrectResultSizeDataAccessException e) {
            // Expected
            return;
        }
        fail( "Should have thrown IncorrectResultSizeDataAccessException" );
    }

    @Test
    public void findByUser_whenInvalidUser_thenReturnNull() {

        PasswordResetToken found = passwordResetTokenRepository.findByUser( createUser( -1 ) );
        assertThat( found ).isNull();
    }

    @Test
    public void deleteAllExpiredSince_whenValidDate_thenDeleteTokens() {
        passwordResetTokenRepository.deleteAllExpiredSince( new Date() );
        assertThat( passwordResetTokenRepository.findAll() ).containsExactly( validToken );

    }

}
