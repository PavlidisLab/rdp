package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

/**
 * Created by mjacobson on 22/01/18.
 */
@Configuration
@ConfigurationProperties(prefix = "rdp.faq")
@PropertySource("${rdp.settings.faq-file}")
@Data
public class FaqSettings {

    private Map<String, String> questions;
    private Map<String, String> answers;

}
