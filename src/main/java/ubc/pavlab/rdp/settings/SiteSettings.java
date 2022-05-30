package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.net.URI;

/**
 * Created by mjacobson on 22/01/18.
 */
@Configuration
@ConfigurationProperties(prefix = "rdp.site")
@Data
public class SiteSettings {

    @NotEmpty
    private URI host;

    private String context;

    public java.net.URI getHostUri() {
        return UriComponentsBuilder.fromUri( host ).path( context ).build().toUri();
    }

    @Email
    @NotEmpty
    private String contactEmail;

    @Email
    @NotEmpty
    private String adminEmail;

    private String gaTracker;
}
