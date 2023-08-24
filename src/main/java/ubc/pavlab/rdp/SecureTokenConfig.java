package ubc.pavlab.rdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubc.pavlab.rdp.security.SecureTokenChallenge;
import ubc.pavlab.rdp.security.UserAgentChallenge;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Configuration for generating secure random tokens.
 *
 * @author poirigui
 */
@Configuration
public class SecureTokenConfig {

    @Bean
    public SecureRandom secureRandom() throws NoSuchAlgorithmException {
        return SecureRandom.getInstance( "SHA1PRNG" );
    }

    @Bean
    public SecureTokenChallenge secureTokenChallenge() {
        return new UserAgentChallenge();
    }
}
