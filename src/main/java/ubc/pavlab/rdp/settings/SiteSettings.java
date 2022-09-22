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

    @NotNull(message = "The host URL must be specified.")
    private URI host;

    public URI getHostUrl() {
        return host;
    }

    /**
     * URL of the main site.
     * <p>
     * If configured, CORS policies will be setup so that scripts running on the main site can freely access the
     * '/stats' and '/api/**' endpoints.
     */
    private URI mainsite;

    @Pattern(regexp = "#[a-fA-F\\d]{6}", message = "The theme color must be a valid hex color (i.e. '#FFFFFF').")
    private String themeColor;

    @Email(message = "The contact email must be valid.")
    @NotEmpty(message = "The contact email must be specified.")
    private String contactEmail;

    @Email(message = "The admin email must be valid.")
    @NotEmpty(message = "The admin email must be specified.")
    private String adminEmail;

    private String gaTracker;
}
