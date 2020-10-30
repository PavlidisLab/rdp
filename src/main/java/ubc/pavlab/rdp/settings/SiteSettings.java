package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.site")
@Getter
@Setter
public class SiteSettings {

    @URL
    private String host;

    private String context;

    public java.net.URI getHostUri() {
        return UriComponentsBuilder.fromUriString( host ).path( context ).build().toUri();
    }

    @Email
    private String contactEmail;

    @Email
    private String adminEmail;

    @URL
    private String proxyHost;
    private String proxyPort;

    private String gaTracker;
}
