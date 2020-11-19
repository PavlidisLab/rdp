package ubc.pavlab.rdp;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ubc.pavlab.rdp.controllers.ApiController;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Collections;

/**
 * Configuration for the public search API.
 */
@Configuration
public class ApiConfig {

    @Autowired
    private SiteSettings siteSettings;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info( new Info().version( ApiController.API_VERSION ) )
                .servers( Collections.singletonList( new Server().url( siteSettings.getHostUri().toString() ) ) );

    }
}
