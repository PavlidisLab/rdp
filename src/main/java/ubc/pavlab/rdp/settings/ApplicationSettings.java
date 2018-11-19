package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.settings")
@Getter
@Setter
public class ApplicationSettings {

    @Getter
    @Setter
    public static class CacheSettings {

        private boolean enabled = true;
        private boolean loadFromDisk;
        private String geneFilesLocation;
        private String termFile;
        private String annotationFile;

    }

    private CacheSettings cache;
    private boolean sendEmailOnRegistration;
    private int goTermSizeLimit = 100;

}
