package ubc.pavlab.rdp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.MacFactory;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
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
    public MacFactory macFactory( ApplicationSettings applicationSettings ) {
        MacFactory factory = new MacFactory();
        factory.setAlgorithm( applicationSettings.getSignatureAlgorithm() );
        factory.setKey( new SecretKeySpec( applicationSettings.getSignatureKey().getBytes( StandardCharsets.UTF_8 ), "HmacSHA256" ) );
        return factory;
    }
}
