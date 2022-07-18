package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.exception.ApiException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides API accesses for remote applications
 * <p>
 * It's worth mentioning that the '/api' endpoint is delegated to springdoc OpenAPI JSON generator and welcome the
 * client with a specification of the endpoints of this API.
 */

@RestController
@CommonsLog
public class ApiController {

    @Autowired
    private MessageSource messageSource;
    @Autowired
    private UserService userService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private GeneInfoService geneService;
    @Autowired
    private OrganInfoService organInfoService;
    @Autowired
    private UserGeneService userGeneService;
    @Autowired
    private ApplicationSettings applicationSettings;
    @Autowired
    private SiteSettings siteSettings;
    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @ExceptionHandler({ AccessDeniedException.class })
    public ResponseEntity<?> handleAccessDeniedException( HttpServletRequest req, Exception e ) {
        log.warn( "Unauthorized access to the API via " + req.getRequestURI() + ".", e );
        return ResponseEntity.status( HttpStatus.UNAUTHORIZED )
                .contentType( MediaType.TEXT_PLAIN )
                .body( e.getMessage() );
    }

    @ExceptionHandler({ ApiException.class })
    public ResponseEntity<String> handleApiException( ApiException e ) {
        return ResponseEntity
                .status( e.getStatus() )
                .contentType( MediaType.TEXT_PLAIN )
                .body( e.getMessage() );
    }

    /**
     * Handle all unmapped API requests with a 404 error.
     */
    @RequestMapping(value = "/api/*")
    public void handleMissingRoute() {
        throw new ApiException( HttpStatus.NOT_FOUND, "No endpoint found for your request URL." );
    }

    /**
     * Provide general statistics about this registry.
     */
    @GetMapping(value = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getStats() {
        checkEnabled();
        return Stats.builder()
                .users( userService.countResearchers() )
                .publicUsers( userService.countPublicResearchers() )
                .usersWithGenes( userGeneService.countUsersWithGenes() )
                .userGenes( userGeneService.countAssociations() )
                .uniqueUserGenes( userGeneService.countUniqueAssociations() )
                .uniqueUserGenesTAll( userGeneService.countUniqueAssociationsAllTiers() )
                .uniqueUserGenesHumanTAll( userGeneService.countUniqueAssociationsToHumanAllTiers() )
                .researchersByTaxa( userGeneService.researcherCountByTaxon() )
                .build();
    }

    /**
     * Retrieve all users in a paginated format.
     * <p>
     * Results that cannot be displayed are anonymized.
     */
    @GetMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<User> getUsers( Pageable pageable,
                                Locale locale ) {
        checkEnabled();
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
            return userService.findByEnabledTrueNoAuth( pageable )
                    .map( user -> permissionEvaluator.hasPermission( auth2, user, "read" ) ? user : userService.anonymizeUser( user ) )
                    .map( user -> initUser( user, locale ) );

        } else {
            return userService.findByEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType.PUBLIC, pageable ).map( user -> initUser( user, locale ) );
        }
    }

    /**
     * Retrieve all genes in a paginated format.
     * <p>
     * Results that cannot be displayed are anonymized.
     */
    @GetMapping(value = "/api/genes", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<UserGene> getGenes( Pageable pageable,
                                    Locale locale ) {
        checkEnabled();
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
            return userGeneService.findByUserEnabledTrueNoAuth( pageable )
                    .map( userGene -> permissionEvaluator.hasPermission( auth2, userGene, "read" ) ? userGene : userService.anonymizeUserGene( userGene ) )
                    .map( userGene -> initUserGene( userGene, locale ) );
        } else {
            return userGeneService.findByUserEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType.PUBLIC, pageable ).map( userGene -> initUserGene( userGene, locale ) );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> searchUsersByName( @RequestParam String nameLike,
                                         @RequestParam Boolean prefix,
                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                         @RequestParam(required = false) Set<String> organUberonIds,
                                         Locale locale ) {
        checkEnabled();
        checkResearcherSearchEnabled();
        if ( prefix ) {
            return initUsers( userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        } else {
            return initUsers( userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> searchUsersByDescription( @RequestParam String descriptionLike,
                                                @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                @RequestParam(required = false) Set<String> organUberonIds,
                                                Locale locale ) {
        checkEnabled();
        checkResearcherSearchEnabled();
        return initUsers( userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
    }

    /**
     * Search for genes by symbol, taxon, tier, orthologs and organ systems.
     */
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserGene> searchUsersByGeneSymbol( @RequestParam String symbol,
                                                   @RequestParam Integer taxonId,
                                                   @RequestParam(required = false) Set<TierType> tiers,
                                                   @RequestParam(required = false) Integer orthologTaxonId,
                                                   @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                   @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                   @RequestParam(required = false) Set<String> organUberonIds,
                                                   Locale locale ) {
        checkEnabled();
        checkGeneSearchEnabled();

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown taxon ID: %s.", taxonId ) );
        }

        GeneInfo gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown gene with symbol: %s.", symbol ) );
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, messageSource.getMessage( "ApiController.noOrthologsWithGivenParameters", null, locale ) );
        }

        return initUserGenes( userGeneService.handleGeneSearch( gene, restrictTiers( tiers ), orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
    }

    /**
     * Search for genes by symbol, taxon identifier, tier, orthologs and organ systems.
     *
     * @deprecated This endpoint is maintained for backward-compatibility. New usages should provide a full set of tiers
     * they expect the search to be realized on.
     */
    @Deprecated
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId", "tier" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UserGene> searchUsersByGeneSymbol( @RequestParam String symbol,
                                                   @RequestParam Integer taxonId,
                                                   @RequestParam String tier,
                                                   @RequestParam(required = false) Integer orthologTaxonId,
                                                   @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                   @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                   @RequestParam(required = false) Set<String> organUberonIds,
                                                   Locale locale ) {
        Set<TierType> tiers;
        if ( tier.equals( "ANY" ) ) {
            tiers = TierType.ANY;
        } else if ( tier.equals( "TIERS1_2" ) ) {
            tiers = TierType.MANUAL;
        } else {
            try {
                tiers = EnumSet.of( TierType.valueOf( tier ) );
            } catch ( IllegalArgumentException e ) {
                log.error( String.format( "Could not parse tier type: %s.", e.getMessage() ) );
                throw new ApiException( HttpStatus.BAD_REQUEST, String.format( locale, "Unknown tier: %s.", tier ), e );
            }
        }

        return searchUsersByGeneSymbol( symbol, taxonId, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, locale );
    }

    @GetMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public User getUserById( @PathVariable Integer userId,
                             Locale locale ) {
        checkEnabled();
        User user = userService.findUserById( userId );
        if ( user == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown user with ID: %d.", userId ) );
        }
        return initUser( user, locale );
    }

    @GetMapping(value = "/api/users/by-anonymous-id/{anonymousId}")
    public User getUserByAnonymousId( @PathVariable UUID anonymousId,
                                      Locale locale ) {
        checkEnabled();
        checkAnonymousResultsEnabled();
        User user = userService.findUserByAnonymousIdNoAuth( anonymousId );
        if ( user == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( "Unknown user with anonymous ID: %s.", anonymousId ) );
        }
        if ( permissionEvaluator.hasPermission( SecurityContextHolder.getContext().getAuthentication(), user, "read" ) ) {
            return initUser( user, locale );
        } else {
            return initUser( userService.anonymizeUser( user ), locale );
        }
    }

    private void checkEnabled() {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, "Public API is not available for this registry." );
        }
    }

    private void checkGeneSearchEnabled() {
        if ( !applicationSettings.getSearch().getEnabledSearchModes().contains( ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER ) ) {
            throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, "Search by gene is not available for this registry." );
        }
    }

    private void checkResearcherSearchEnabled() {
        if ( !applicationSettings.getSearch().getEnabledSearchModes().contains( ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER ) ) {
            throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, "Search by researcher is not available for this registry." );
        }
    }

    private void checkAnonymousResultsEnabled() {
        if ( !applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, "Anonymized search results is not available for this registry." );
        }
    }

    private List<UserGene> initUserGenes( List<UserGene> genes, Locale locale ) {
        for ( UserGene gene : genes ) {
            initUserGene( gene, locale );
        }
        return genes;
    }

    private UserGene initUserGene( UserGene gene, Locale locale ) {
        // Initializing for the json serializer.
        initUser( gene.getUser(), locale );
        gene.getUser().getUserGenes().clear();
        gene.setRemoteUser( gene.getUser() );
        return gene;
    }

    private List<User> initUsers( List<User> users, Locale locale ) {
        for ( User user : users ) {
            this.initUser( user, locale );
        }
        return users;
    }

    private User initUser( User user, Locale locale ) {
        user.setOrigin( messageSource.getMessage( "rdp.site.shortname", null, locale ) );
        user.setOriginUrl( siteSettings.getHostUri() );
        return user;
    }

    /**
     * We do not want to query TIER3 genes internationally, so if such request arrives, we have to either
     * try to transform it to only include TIER1&2, or prevent the search.
     *
     * @param tiers the tier type to be restricted to not include tier 3.
     * @return manual (tier1&2) for tier type ANY, or throws an exception if tier type was specifically 3.
     */
    private Set<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

    private Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }

}
