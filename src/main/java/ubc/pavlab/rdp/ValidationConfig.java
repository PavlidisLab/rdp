package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import ubc.pavlab.rdp.validation.AllowedDomainStrategy;
import ubc.pavlab.rdp.validation.EmailValidator;
import ubc.pavlab.rdp.validation.RecaptchaValidator;
import ubc.pavlab.rdp.validation.ResourceBasedAllowedDomainStrategy;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

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
        if ( allowedEmailDomainsFile != null ) {
            log.info( "Reading allowed email domains from " + allowedEmailDomainsFile + "..." );
            strategy = new ResourceBasedAllowedDomainStrategy( allowedEmailDomainsFile, refreshDelay );
            ( (ResourceBasedAllowedDomainStrategy) strategy ).refresh();
            Set<String> allowedDomains = ( (ResourceBasedAllowedDomainStrategy) strategy ).getAllowedDomains();
            if ( allowedDomains.size() <= 5 ) {
                log.info( String.format( "Email validation is configured to accept only addresses from: %s.", String.join( ", ", allowedDomains ) ) );
            } else {
                log.info( String.format( "Email validation is configured to accept only addresses from a list of %d domains.", allowedDomains.size() ) );
            }
        } else {
            strategy = ( domain ) -> true;
            log.warn( "No allowed email domains file specified, all domains will be allowed for newly registered users." );
        }
        return new EmailValidator( strategy, allowIdn );
    }

    @Bean
    @ConditionalOnProperty("rdp.site.recaptcha-secret")
    public RecaptchaValidator recaptchaValidator( @Value("${rdp.site.recaptcha-secret}") String secret ) {
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().add( new FormHttpMessageConverter() );
        return new RecaptchaValidator( rt, secret );
    }
}
