package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.faq")
@Getter
@Setter
public class FaqSettings {

    private Map<String, String> questions;
    private Map<String, String> answers;

}
