package ubc.pavlab.rdp;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the public search API.
 */
@Configuration
public class ApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info( new Info().version( "1.4.0" ) );
    }
}
