package ubc.pavlab.rdp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.OntologyMessageSource;

/**
 * Created by mjacobson on 16/01/18.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SiteSettings siteSettings;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * This bean provides message source resolution without {@link OntologyMessageSource}.
     */
    @Bean
    public HierarchicalMessageSource messageSourceWithoutOntology() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        // application-prod.properties and login.properties is there for backward compatibility since
        // we used to pull locale strings from there.
        messageSource.setBasenames( "file:messages", "file:application-prod", "file:login", "classpath:messages" );
        return messageSource;
    }

    @Bean
    public MessageSource messageSource( OntologyMessageSource ontologyMessageSource ) {
        HierarchicalMessageSource messageSource = messageSourceWithoutOntology();
        // if it cannot be resolved in messages.properties, then lookup some of our built-in resolution for
        // ontology-related patterns
        messageSource.setParentMessageSource( ontologyMessageSource );
        return messageSource;
    }

    /**
     * CORS configuration for the /stats endpoint so that statistics can be queried from the main website.
     */
    @Override
    public void addCorsMappings( CorsRegistry registry ) {
        if ( siteSettings.getMainsite() != null ) {
            registry.addMapping( "/stats" )
                    .allowedOrigins( siteSettings.getMainsite().getScheme() + "://" + siteSettings.getMainsite().getAuthority() )
                    .allowedMethods( "GET" );
        }
    }
}