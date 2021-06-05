package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by mjacobson on 22/01/18.
 */
@Configuration
@ConfigurationProperties(prefix = "rdp.site")
@Data
public class SiteSettings {

    private String host;

    private String context;

    public java.net.URI getHostUri() {
        return UriComponentsBuilder.fromUriString( host ).path( context ).build().toUri();
    }

    private String contactEmail;

    private String adminEmail;

    private String proxyHost;
    private String proxyPort;

    private String gaTracker;
}
