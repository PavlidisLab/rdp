package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    @Email
    private String contactEmail;

    @Email
    private String adminEmail;

    @SneakyThrows
    public java.net.URL getFullUrl() {
        return new java.net.URL( new java.net.URL( host ), context );
    }

    @URL
    private String proxyHost;
    private String proxyPort;

}
