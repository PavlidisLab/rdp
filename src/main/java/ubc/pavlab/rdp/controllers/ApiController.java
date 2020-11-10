package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.exception.ApiException;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides API access for remote applications
 * <p>
 * It's worth mentioning that the '/api' endpoint is delegated to springdoc OpenAPI JSON generator and welcome the
 * client with a specification of the endpoints of this API.
 */

@RestController
@CommonsLog
public class ApiController {

    private static final String API_VERSION = "1.0.0"; //TODO update every time there is any change in how the API works.

    @Autowired
    MessageSource messageSource;
    @Autowired
    private UserService userService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private GeneInfoService geneService;
    @Autowired
    TierService tierService;
    @Autowired
    private UserOrganService userOrganService;
    @Autowired
    UserGeneService userGeneService;
    @Autowired
    OrganInfoService organInfoService;
    @Autowired
    private ApplicationSettings applicationSettings;
    @Autowired
    private SiteSettings siteSettings;
    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @ExceptionHandler({ AuthenticationException.class })
    public ResponseEntity handleAuthenticationException() {
        return ResponseEntity.status( HttpStatus.UNAUTHORIZED ).build();
    }

    @ExceptionHandler({ ApiException.class })
    public ResponseEntity handleApiException( ApiException e ) {
        return ResponseEntity.status( e.getStatus() ).body( e.getMessage() );
    }

    /**
     * Fallback for unmapped sub-paths.
     *
     * @return 404.
     */
    @GetMapping(value = "/api/*", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchApiInfo() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Provide general statistics about this registry.
     */
    @GetMapping(value = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getStats() {
        return new Stats( userService.countResearchers(),
                userGeneService.countUsersWithGenes(),
                userGeneService.countAssociations(),
                userGeneService.countUniqueAssociations(),
                userGeneService.countUniqueAssociationsAllTiers(),
                userGeneService.countUniqueAssociationsToHumanAllTiers(),
                userGeneService.researcherCountByTaxon() );
    }

    /**
     * Hides the default users search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @GetMapping(value = "/api/users/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsers() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Hides the default genes search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @GetMapping(value = "/api/genes/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchGenes() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByName( @RequestParam String nameLike,
                                     @RequestParam Boolean prefix,
                                     @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                     @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                     @RequestParam(required = false) Set<String> organUberonIds,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                     @Deprecated @RequestParam(required = false) String auth,
                                     Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        if ( prefix ) {
            return initUsers( userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        } else {
            return initUsers( userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByDescription( @RequestParam String descriptionLike,
                                            @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                            @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                            @RequestParam(required = false) Set<String> organUberonIds,
                                            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                            @Deprecated @RequestParam(required = false) String auth,
                                            Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        return initUsers( userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
    }

    /**
     * Search for genes by symbol, taxon, tier, orthologs and organ systems.
     */
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByGeneSymbol( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @Deprecated @RequestParam(required = false) String auth,
                                           Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }

        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            return ResponseEntity.notFound().build();
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );

        Collection<UserGene> orthologs = userGeneService.handleOrthologSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            return new ResponseEntity<>( messageSource.getMessage( "ApiController.noOrthologsWithGivenParameters", null, locale ), null, HttpStatus.NOT_FOUND );
        }

        return initGeneUsers( userGeneService.handleGeneSearch( gene, restrictTiers( tiers ), orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
    }

    /**
     * Search for genes by symbol, taxon identifier, tier, orthologs and organ systems.
     *
     * @deprecated This endpoint is maintained for backward-compatibility. New usages should provide a full set of tiers
     * they expect the search to be realized on.
     */
    @Deprecated
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId", "tier" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByGeneSymbol( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam String tier,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @Deprecated @RequestParam(required = false) String auth,
                                           Locale locale ) {
        Set<TierType> tiers;
        if ( tier.equals( "TIER_ANY" ) ) {
            tiers = TierType.ANY;
        } else if ( tier.equals( "TIER1_2" ) || tier.equals( "TIER_MANUAL" ) ) {
            tiers = TierType.MANUAL;
        } else {
            try {
                tiers = EnumSet.of( TierType.valueOf( tier ) );
            } catch ( IllegalArgumentException e ) {
                log.error( e );
                return ResponseEntity.badRequest().body( MessageFormat.format( "Unknown tier {0}.", tier ) );
            }
        }

        return searchUsersByGeneSymbol( symbol, taxonId, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, authorizationHeader, auth, locale );
    }

    @GetMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getUserById( @PathVariable Integer userId,
                               @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                               @RequestParam(name = "auth", required = false) String auth,
                               Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        User user = userService.findUserById( userId );
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        return initUser( user, locale );
    }

    @GetMapping(value = "/api/users/by-anonymous-id/{anonymousId}")
    public Object getUserByAnonymousId( @PathVariable UUID anonymousId,
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                        @RequestParam(name = "auth", required = false) String auth,
                                        Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        User user = userService.findUserByAnonymousId( anonymousId );
        if ( user == null ) {
            return ResponseEntity.notFound().build();
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

    private void checkAuth( String authorizationHeader, String authToken ) throws AuthenticationException {
        if ( authToken == null && authorizationHeader != null ) {
            String[] pieces = authorizationHeader.split( " ", 2 );
            if ( pieces.length == 2 && pieces[0].equalsIgnoreCase( "Bearer" ) ) {
                authToken = pieces[1];
            } else {
                throw new ApiException( HttpStatus.BAD_REQUEST, "Cannot parse Authorization header, should be 'Bearer <api_key>'." );
            }
        }

        User u;
        if ( authToken == null ) {
            // anonymous user which is the default for spring security
            return;
        } else if ( applicationSettings.getIsearch().getAuthTokens().contains( authToken ) ) {
            // remote admin authentication
            u = userService.getRemoteAdmin();
            if ( u == null ) {
                throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, messageSource.getMessage( "ApiController.misconfiguredRemoteAdmin", null, Locale.getDefault() ) );
            }
        } else {
            // authentication via access token
            try {
                u = userService.findUserByAccessTokenNoAuth( authToken );
            } catch ( TokenException e ) {
                throw new BadCredentialsException( "Invalid API token.", e );
            }
        }

        if ( u == null ) {
            throw new BadCredentialsException( "No user associated to the provided API token." );
        }

        UserPrinciple principle = new UserPrinciple( u );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() ) );
    }

    private Collection<UserGene> initGeneUsers( Collection<UserGene> genes, Locale locale ) {
        for ( UserGene gene : genes ) {
            // Initializing for the json serializer.
            initUser( gene.getUser(), locale );
            gene.getUser().getUserGenes().clear();
            gene.setRemoteUser( gene.getUser() );
        }
        return genes;
    }

    private Collection<User> initUsers( Collection<User> users, Locale locale ) {
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

    private Collection<UserOrgan> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : userOrganService.findByUberonIdIn( organUberonIds );
    }

}
