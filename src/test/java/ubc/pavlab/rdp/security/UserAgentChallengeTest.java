package ubc.pavlab.rdp.security;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.PasswordResetToken;
import ubc.pavlab.rdp.model.Token;

@RunWith(SpringRunner.class)
public class UserAgentChallengeTest {

    @TestConfiguration
    static class UserAgentChallengeTestContextConfiguration {

        @Bean
        public UserAgentChallenge userAgentChallenge() {
            return new UserAgentChallenge();
        }
    }

    @Autowired
    private UserAgentChallenge userAgentChallenge;

    @Test
    public void challenge() throws TokenException {
        Token token = new PasswordResetToken();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/116.0" );
        userAgentChallenge.challenge( token, request );
    }

    @Test(expected = TokenException.class)
    public void challenge_whenAgentIsABot() throws TokenException {
        Token token = new PasswordResetToken();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader( "User-Agent", "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/W.X.Y.Z Mobile Safari/537.36 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)" );
        userAgentChallenge.challenge( token, request );
    }
}