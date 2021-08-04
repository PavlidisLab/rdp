package ubc.pavlab.rdp;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubc.pavlab.rdp.controllers.ApiController;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Collections;
import java.util.Locale;

/**
 * Configuration for the public search API.
 */
@Configuration
public class ApiConfig {

    @Autowired
    private SiteSettings siteSettings;

    @Bean
    public OpenAPI openAPI( MessageSource messageSource ) {
        // FIXME: retrieve that from the request context
        Locale locale = Locale.getDefault();
        String shortname = messageSource.getMessage( "rdp.site.shortname", null, locale );
        return new OpenAPI()
                .info( new Info()
                        .title( messageSource.getMessage( "ApiConfig.title", new String[]{ shortname }, locale ) )
                        .description( messageSource.getMessage( "ApiConfig.description", new String[]{ shortname }, locale ) )
                        .contact( new Contact().email( siteSettings.getContactEmail() ) )
                        .version( ApiController.API_VERSION ) )
                .servers( Collections.singletonList( new Server().url( siteSettings.getHostUri().toString() ) ) );

    }
}
