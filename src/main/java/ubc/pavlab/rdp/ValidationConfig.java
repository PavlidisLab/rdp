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
import ubc.pavlab.rdp.validation.*;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This configuration provides a few {@link org.springframework.validation.Validator} beans.
 */
@CommonsLog
@Configuration
public class ValidationConfig {

    @Bean
    public EmailValidator emailValidator(
            @Value("${rdp.settings.allowed-email-domains}") List<String> allowedEmailDomains,
            @Value("${rdp.settings.allowed-email-domains-file}") Resource allowedEmailDomainsFile,
            @Value("${rdp.settings.allowed-email-domains-file-refresh-delay}") @DurationUnit(ChronoUnit.SECONDS) Duration refreshDelay,
            @Value("${rdp.settings.allow-internationalized-email-domains}") boolean allowIdn ) throws IOException {
        List<AllowedDomainStrategy> strategies = new ArrayList<>();
        if ( allowedEmailDomains != null && !allowedEmailDomains.isEmpty() ) {
            SetBasedAllowedDomainStrategy strategy = new SetBasedAllowedDomainStrategy( allowedEmailDomains );
            strategies.add( strategy );
            log.info( String.format( "Email validation is configured to accept addresses from: %s.", String.join( ", ",
                    strategy.getAllowedDomains() ) ) );
        }
        if ( allowedEmailDomainsFile != null ) {
            log.info( "Reading allowed email domains from " + allowedEmailDomainsFile + "..." );
            if ( refreshDelay.isZero() ) {
                log.warn( "The refresh delay for reading " + allowedEmailDomainsFile + " is set to zero: the file will be re-read for every email domain validation." );
            }
            ResourceBasedAllowedDomainStrategy strategy = new ResourceBasedAllowedDomainStrategy( allowedEmailDomainsFile, refreshDelay );
            strategy.refresh();
            Set<String> allowedDomains = strategy.getAllowedDomains();
            strategies.add( strategy );
            if ( strategy.getAllowedDomains().size() <= 5 ) {
                log.info( String.format( "Email validation is configured to accept addresses from: %s.", String.join( ", ", allowedDomains ) ) );
            } else {
                log.info( String.format( "Email validation is configured to accept addresses from a list of %d domains.", allowedDomains.size() ) );
            }
        }
        AllowedDomainStrategy strategy;
        if ( strategies.isEmpty() ) {
            strategy = ( domain ) -> true;
            log.warn( "No allowed email domains file specified, all domains will be allowed for newly registered users." );
        } else if ( strategies.size() == 1 ) {
            strategy = strategies.iterator().next();
        } else {
            strategy = domain -> strategies.stream().anyMatch( s -> s.allows( domain ) );
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
