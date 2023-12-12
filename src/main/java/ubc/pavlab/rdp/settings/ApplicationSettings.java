package ubc.pavlab.rdp.settings;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.ontology.resolvers.OntologyResolver;
import ubc.pavlab.rdp.services.GeneInfoService;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by mjacobson on 22/01/18.
 */
@Validated
@Configuration("applicationSettings")
@ConfigurationProperties(prefix = "rdp.settings")
@Data
public class ApplicationSettings {

    @Data
    public static class CacheSettings {

        /**
         * Enable loading and scheduled update of cached data.
         */
        private boolean enabled;
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
        private String termFile;
        /**
         * Location of gene2go annotations.
         * <p>
         * FIXME: use a {@link Resource}, but resolving is not supported at the config-level (see <a href="https://github.com/PavlidisLab/rdp/pull/192">#192</a>)
         */
        private Resource annotationFile;
        /**
         * Location of gene orthologs.
         */
        private Resource orthologFile;
        /**
         * Location of organ system terms.
         * <p>
         * FIXME: use a {@link Resource}, but resolving is not supported at the config-level (see <a href="https://github.com/PavlidisLab/rdp/pull/192">#192</a>)
         */
        private String organFile;

    }

    @Data
    public static class ProfileSettings {
        /**
         * Enabled researcher positions under the user profile.
         */
        private EnumSet<ResearcherPosition> enabledResearcherPositions;
        /**
         * Enabled researcher categories under the user profile.
         */
        private EnumSet<ResearcherCategory> enabledResearcherCategories;
    }

    @Data
    public static class OrganSettings {
        /**
         * Enable organ systems.
         */
        private boolean enabled;
    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Data
    public static class PrivacySettings {
        /**
         * Default privacy level for new user accounts.
         * <p>
         * Must be one of {@link #enabledLevels}.
         */
        @Min(0)
        @Max(2)
        private int defaultLevel;
        /**
         * Minimum level of privacy for user profiles.
         *
         * @deprecated The setting is still honored, but you should use {@link #enabledLevels} instead.
         */
        @Deprecated
        @Min(0)
        @Max(2)
        private int minLevel;
        /**
         * List of enabled privacy levels for user profiles.
         * <p>
         * At least one level must be enabled and one of the must be used for {@link #defaultLevel}.
         */
        @Size(min = 1, message = "There must be at least one enabled privacy level.")
        private EnumSet<PrivacyLevelType> enabledLevels;
        /**
         * List of enabled privacy levels for user-associated genes.
         * <p>
         * If no levels are enabled, the profile value will be cascaded.
         */
        private EnumSet<PrivacyLevelType> enabledGeneLevels;
        /**
         * Indicate if user profiles are shared publicly by default.
         */
        private boolean defaultSharing;
        /**
         * Whether or not privacy settings are customizable at profile-level.
         */
        private boolean customizableLevel;
        /**
         * Whether or not privacy settings are customizable at gene-level.
         * <p>
         * It's possible to customize gene privacy even if the profile levels are not customizable.
         */
        private boolean customizableGeneLevel;
        /**
         * Allow users to choose whether or not their data is shared publicly.
         */
        private boolean customizableSharing;
        /**
         * Enable public search from unauthenticated users.
         */
        private boolean publicSearch;
        /**
         * Enable registered search from authenticated users.
         */
        private boolean registeredSearch;
        /**
         * Allow users to choose whether or not their genes can be hidden from their public profile.
         */
        private boolean allowHideGenelist;
        /**
         * Allow anonymized search results to be displayed.
         */
        private boolean enableAnonymizedSearchResults;
    }

    @Data
    public static class SearchSettings {

        public enum SearchMode {
            BY_GENE,
            BY_RESEARCHER
        }

        /**
         * Enabled search modes in the search user interface and API.
         * <p>
         * The order of elements indicates the order of display in the search interface.
         */
        @Size(min = 1, message = "There must be at least one enabled search mode.")
        private LinkedHashSet<SearchMode> enabledSearchModes;
    }

    @SuppressWarnings("WeakerAccess") //Used in frontend
    @Data
    public static class InternationalSearchSettings {
        /**
         * Whether partner search is enabled.
         */
        private boolean enabled;
        /**
         * Whether partner search is enabled by default in the search interface.
         */
        private boolean defaultOn;
        /**
         * User ID for the remote search user, or null to disable.
         * <p>
         * If set, the user must exist at startup and there must be at least one {@link #authTokens} specified.
         */
        private Integer userId;
        /**
         * List of partner APIs endpoints.
         */
        private URI[] apis;
        /**
         * List of tokens used to authorize partner requests. If the token is matched, the operations will be performed
         * by the user with ID {@link #userId}.
         */
        private List<String> authTokens;
        /**
         * Token used to query other partner registries.
         */
        private String searchToken;
        /**
         * Request timeout when querying partner registries, or null to disable.
         * <p>
         * When formatted as a number, it is interpreted as a number of seconds. You may also use a unit suffix (i.e. '100ms')
         * to specify a precise amount of time.
         *
         * @deprecated use {@link #connectTimeout} and {@link #readTimeout} instead which provide more granularity in
         * defining the timeout.
         */
        @Deprecated
        @DurationUnit(value = ChronoUnit.SECONDS)
        private Duration requestTimeout;
        /**
         * Connection timeout in milliseconds, or null to disable.
         */
        @DurationUnit(value = ChronoUnit.MILLIS)
        private Duration connectTimeout;
        /**
         * Read timeout in milliseconds, or null to disable.
         */
        @DurationUnit(value = ChronoUnit.MILLIS)
        private Duration readTimeout;
    }

    @Data
    public static class OntologySettings {
        /**
         * Enable ontologies.
         */
        private boolean enabled;
        /**
         * Default resolver used for resolving ontologies.
         */
        private Class<? extends OntologyResolver> defaultResolver;
        @Size(min = 1, max = Ontology.MAX_NAME_LENGTH)
        private String reactomePathwaysOntologyName;
        private Resource reactomePathwaysFile;
        private Resource reactomePathwaysHierarchyFile;
        private Resource reactomeStableIdentifiersFile;
        private URI reactomeContentServiceUrl;
    }

    private ProfileSettings profile;
    private SearchSettings search;
    private InternationalSearchSettings isearch;
    private PrivacySettings privacy;
    private CacheSettings cache;
    private OrganSettings organs;
    private OntologySettings ontology;

    private Resource faqFile;
    private boolean sendEmailOnRegistration;
    /**
     * Maximum number of GO terms.
     */
    @Min(0)
    private long goTermSizeLimit;
    /**
     * Enabled tier types.
     */
    public EnumSet<TierType> enabledTiers;
    /**
     * List of allowed email domains for registering users.
     * <p>
     * May be null or empty, in which case any email address will be allowed.
     */
    private List<String> allowedEmailDomains;
    /**
     * File containing allowed email domains for registering users.
     * <p>
     * May be null, in which case any email address will be allowed.
     */
    private Resource allowedEmailDomainsFile;
    /**
     * Refresh delay to reload the allowed email domains file, in seconds.
     */
    @DurationUnit(value = ChronoUnit.SECONDS)
    private Duration allowedEmailDomainsFileRefreshDelay;
    /**
     * Allow <a href="https://en.wikipedia.org/wiki/Internationalized_domain_name">internationalized domain names</a>.
     * If set to true, Punycode can be added to {@link #allowedEmailDomains} or {@link #allowedEmailDomainsFile}.
     */
    private boolean allowInternationalizedEmailDomains;
}
