package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestTemplate;
import ubc.pavlab.rdp.validation.AllowedDomainStrategy;
import ubc.pavlab.rdp.validation.EmailValidator;
import ubc.pavlab.rdp.validation.RecaptchaValidator;
import ubc.pavlab.rdp.validation.ResourceBasedAllowedDomainStrategy;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * This configuration provides a few {@link org.springframework.validation.Validator} beans.
 */
@CommonsLog
@Configuration
public class ValidationConfig {

    @Bean
    public EmailValidator emailValidator(
            @Value("${rdp.settings.allowed-email-domains-file}") Resource allowedEmailDomainsFile,
            @Value("${rdp.settings.allowed-email-domains-refresh-delay}") @DurationUnit(ChronoUnit.SECONDS) Duration refreshDelay,
            @Value("${rdp.settings.allow-internationalized-domain-names}") boolean allowIdn ) throws IOException {
        AllowedDomainStrategy strategy;
        if ( allowedEmailDomainsFile == null ) {
            strategy = ( domain ) -> true;
            log.info( "No allowed email domains file specified, all domains will be allowed for newly registered users." );
        } else {
            log.info( "Reading allowed email domains from " + allowedEmailDomainsFile + "..." );
            strategy = new ResourceBasedAllowedDomainStrategy( allowedEmailDomainsFile, refreshDelay );
            ( (ResourceBasedAllowedDomainStrategy) strategy ).refresh();
        }
        return new EmailValidator( strategy, allowIdn );
    }

    @Bean
    public RecaptchaValidator recaptchaValidator( @Value("${rdp.settings.recaptcha.secret}") String secret ) {
        return new RecaptchaValidator( new RestTemplate(), secret );
    }
}
