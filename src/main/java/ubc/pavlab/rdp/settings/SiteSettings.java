package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.net.URI;

/**
 * Created by mjacobson on 22/01/18.
 */
@Validated
@Configuration
@ConfigurationProperties(prefix = "rdp.site")
@Data
public class SiteSettings {

    @NotNull
    private URI host;

    private String context;

    public URI getHostUri() {
        return UriComponentsBuilder.fromUri( host ).path( context ).build().toUri();
    }

    /**
     * URL of the main site.
     */
    private URI mainsite;

    @Pattern(regexp = "#[a-fA-F\\d]{6}")
    private String themeColor;
    @Email
    @NotEmpty
    private String contactEmail;

    @Email
    @NotEmpty
    private String adminEmail;

    private String gaTracker;
}
