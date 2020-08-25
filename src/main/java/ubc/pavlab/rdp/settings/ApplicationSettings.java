package ubc.pavlab.rdp.settings;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.services.UserService;

import java.util.List;

/**
 * Created by mjacobson on 22/01/18.
 */
@Component
@ConfigurationProperties(prefix = "rdp.settings")
@Data
public class ApplicationSettings {

    @Data
    public static class CacheSettings {

        private boolean enabled = true;
        private boolean loadFromDisk;
        private String geneFilesLocation;
        private String termFile;
        private String annotationFile;
        private String orthologFile;

    }

    @Data
    public static class OrganSettings {
        /**
         * Enable organ systems.
         */
        private Boolean enabled;
    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Data
    public static class PrivacySettings {
        /**
         * Must be one of enabledLevels
         */
        private Integer defaultLevel = PrivacyLevelType.PRIVATE.ordinal();
        /**
         * Minimum level of privacy for user profiles.
         *
         * @deprecated The setting is still honored, but you should use enabledLevels instead.
         */
        @Deprecated
        private Integer minLevel = PrivacyLevelType.PRIVATE.ordinal();
        /**
         * List of enabled privacy levels for user profiles.
         */
        private List<String> enabledLevels;
        /**
         * List of enabled privacy levels for user-associated genes.
         */
        private List<String> enabledGeneLevels;
        private boolean displayMatchAnonymously = false;
        private boolean defaultSharing = false;
        /**
         * Whether or not privacy settings are customizable at profile-level.
         */
        private boolean customizableLevel = true;
        /**
         * Whether or not privacy settings are customizable at gene-level.
         * <p>
         * It's possible to customize gene privacy even if the profile levels are not customizable.
         */
        private boolean customizableGeneLevel = false;
        private boolean customizableSharing = true;
        private boolean publicSearch = false;
        private boolean registeredSearch = false;
        private boolean allowHideGenelist = false;
    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Data
    public static class InternationalSearchSettings {
        private boolean enabled = false;
        private boolean defaultOn = false;
        private Integer userId = 1;
        private String[] apis;
        private List<String> authTokens;
        private String searchToken;
        @URL
        private String host = null;
        private String port = null;
    }

    private InternationalSearchSettings isearch;
    private PrivacySettings privacy;
    private CacheSettings cache;
    private boolean sendEmailOnRegistration;
    private int goTermSizeLimit = 100;
    public List<String> enabledTiers;
    private OrganSettings organs;

//    @Getter
//    @Setter
//    public static class ProxySettings {
//        @URL
//        private String host = null;
//        private String port = null;
//    }
//    private ProxySettings proxy;

}
