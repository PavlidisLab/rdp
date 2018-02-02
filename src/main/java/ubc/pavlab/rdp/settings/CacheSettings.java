package ubc.pavlab.rdp.settings;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.cache")
@Getter
@Setter
public class CacheSettings {

    private boolean loadFromDisk;
    private String geneFilesLocation;
    private String termFile;
    private String annotationFile;

}
