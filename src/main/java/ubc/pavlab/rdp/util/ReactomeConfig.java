package ubc.pavlab.rdp.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ReactomeConfig {

    @Bean
    public RestTemplate reactomeRestTemplate() {
        return new RestTemplate();
    }
}
