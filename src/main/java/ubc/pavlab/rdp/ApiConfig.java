package ubc.pavlab.rdp;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Locale;

/**
 * Configuration for the public search API.
 */
@Configuration
public class ApiConfig {

    @Bean
    public OpenAPI openAPI( SiteSettings siteSettings, BuildProperties buildProperties,
                            MessageSource messageSource, ServletContext servletContext ) {
        // FIXME: retrieve that from the request context
        Locale locale = Locale.getDefault();
        String shortname = messageSource.getMessage( "rdp.site.shortname", null, locale );
        return new OpenAPI()
                .info( new Info()
                        .title( messageSource.getMessage( "ApiConfig.title", new String[]{ shortname }, locale ) )
                        .description( messageSource.getMessage( "ApiConfig.description", new String[]{ shortname }, locale ) )
                        .contact( new Contact().email( siteSettings.getContactEmail() ) )
                        .termsOfService( StringUtils.isEmpty( messageSource.getMessage( "rdp.privacy-policy", null, locale ) ) ?
                                null : UriComponentsBuilder.fromPath( servletContext.getContextPath() ).path( "/terms-of-service" ).toUriString() )
                        .version( buildProperties.getVersion() ) )
                .servers( Collections.singletonList( new Server().url( siteSettings.getHostUrl().toString() ) ) );

    }
}
