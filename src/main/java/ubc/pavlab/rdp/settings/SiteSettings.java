package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
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
    private String fullname;
    private String shortname;
    @URL
    private String mainsite;
    @Email
    private String contactEmail;
    @Email
    private String adminEmail;

    private String logohtml;

    public String getFullUrl() {
        return host + context + (context.endsWith( "/" ) ? "" : "/");
    }

}
