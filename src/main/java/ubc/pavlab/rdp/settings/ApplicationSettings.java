package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.services.GeneInfoService;

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

        /**
         * Enable loading and scheduled update of cached data.
         */
        private boolean enabled = true;
        /**
         * Load resources from the disk.
         * <p>
         * This is only used for {@link GeneInfoService} to indicate that the gene_url field should be ignored in the
         * {@link GeneInfo} model.
         */
        private boolean loadFromDisk;
        /**
         * Locations of gene files.
         */
        private Resource geneFilesLocation;
        /**
         * Location of GO terms.
         */
        private Resource termFile;
        /**
         * Location of gene2go annotations.
         */
        private Resource annotationFile;
        /**
         * Location of gene orthologs.
         */
        private Resource orthologFile;
        /**
         * Location of organ system terms.
         */
        private Resource organFile;

    }

    @Data
    public static class ProfileSettings {
        /**
         * Enabled researcher positions under the user profile.
         */
        private List<String> enabledResearcherPositions;
        /**
         * Enabled researcher categories under the user profile.
         */
        private List<String> enabledResearcherCategories;
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
        /**
         * Indicate if user profiles are shared publicly by default.
         */
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
        /**
         * Allow users to choose whether or not their data is shared publicly.
         */
        private boolean customizableSharing = true;
        /**
         * Enable public search from unauthenticated users.
         */
        private boolean publicSearch = false;
        /**
         * Enable registered search from authenticated users.
         */
        private boolean registeredSearch = false;
        /**
         * Allow users to choose whether or not their genes can be hidden from their public profile.
         */
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

    private ProfileSettings profile;
    private InternationalSearchSettings isearch;
    private PrivacySettings privacy;
    private CacheSettings cache;
    private OrganSettings organs;

    private boolean sendEmailOnRegistration;
    private int goTermSizeLimit = 100;
    public List<String> enabledTiers;

}
