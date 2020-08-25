package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.exception.TierException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.ws.rs.core.MediaType;
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

    /**
     * Fallback for unmapped sub-paths.
     *
     * @return 404.
     */
    @GetMapping(value = "/api/*", produces = MediaType.APPLICATION_JSON)
    public Object searchApiInfo() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Hides the default users search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @GetMapping(value = "/api/users/search", produces = MediaType.APPLICATION_JSON)
    public Object searchUsers() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Hides the default genes search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @RequestMapping(value = "/api/genes/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    public Object searchGenes() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike" }, produces = MediaType.APPLICATION_JSON)
    public Object searchUsersByName( @RequestParam String nameLike,
                                     @RequestParam Boolean prefix,
                                     @RequestParam(required = false) Set<String> organUberonIds,
                                     @RequestParam(required = false) String auth,
                                     Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        return initUsers( prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike ), locale );
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON)
    public Object searchUsersByDescription( @RequestParam String descriptionLike,
                                            @RequestParam(required = false) String auth,
                                            @RequestParam(required = false) Set<String> organUberonIds,
                                            Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        return initUsers( userService.findByDescription( descriptionLike ), locale );
    }

    /**
     * Search for genes by symbol, taxon, tier, orthologs and organ systems.
     */
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId", "tiers" }, produces = MediaType.APPLICATION_JSON)
    public Object searchUsersByGeneSymbol( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam Set<TierType> tiers,
                                           @RequestParam(required = false) String auth,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        Optional<Collection<UserOrgan>> organs = Optional.empty();
        if ( organUberonIds != null ) {
            organs = Optional.of( userOrganService.findByUberonIdIn( organUberonIds ) );
        }

        if ( gene == null )
            return new ResponseEntity<>( "Unknown gene.", null,
                    HttpStatus.NOT_FOUND );

        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologs( gene, tiers ) :
                userGeneService.findOrthologsWithTaxon( gene, tiers, taxonService.findById( orthologTaxonId ) );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            return new ResponseEntity<>( messageSource.getMessage( "ApiController.noOrthologsWithGivenParameters", null, locale ), null, HttpStatus.NOT_FOUND );
        }

        try {
            return initGeneUsers( userGeneService.handleGeneSearch( gene, restrictTiers( tiers ), orthologs, organs ), locale );
        } catch ( TierException e ) {
            return new ResponseEntity<>( messageSource.getMessage( "ApiController.tier3GenesNotPublishable", null, locale ), null, HttpStatus.NOT_FOUND );
        }
    }

    /**
     * Search for genes by symbol, taxon identifier, tier, orthologs and organ systems.
     *
     * @deprecated This endpoint is maintained for backward-compatibility. New usages should provide a full set of tiers
     * they expect the search to be realized on.
     */
    @Deprecated
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId", "tier" }, produces = MediaType.APPLICATION_JSON)
    public Object searchUsersByGeneSymbol( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam String tier,
                                           @RequestParam(name = "auth", required = false) String auth,
                                           @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        Set<TierType> tiers;
        if ( tier.equals( "TIER_ANY" ) ) {
            tiers = tierService.getEnabledTiers();
        } else if ( tier.equals( "TIER1_2" ) || tier.equals( "TIER_MANUAL" ) ) {
            tiers = TierType.MANUAL;
        } else if ( TierType.valueOf( tier ) != null ) {
            tiers = EnumSet.of( TierType.valueOf( tier ) );
        } else {
            // FIXME: handle this error properly
            throw new RuntimeException( "Unknown tier " + tier + "." );
        }
        return searchUsersByGeneSymbol( symbol, taxonId, tiers, auth, orthologTaxonId, organUberonIds, locale );
    }

    @GetMapping(value = "/api/organs/search", params = { "description" }, produces = MediaType.APPLICATION_JSON)
    public Object searchOrgansByDescription( String description ) {
        return userOrganService.findByDescription( description );
    }

    private Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Collection<? extends Gene> orthologs ) {
        Collection<UserGene> uGenes = new LinkedList<>();
        if ( orthologs != null && !orthologs.isEmpty() ) {
            for ( Gene ortholog : orthologs ) {
                uGenes.addAll( handleGeneSearch( ortholog, tiers, null ) );
            }
            return uGenes;
        } else {
            return userGeneService.findByGeneIdAndTierIn( gene.getGeneId(), tiers );
        }
    }

    @GetMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON)
    public Object getUserById( @PathVariable Integer userId,
                               @RequestParam(name = "auth", required = false) String auth, Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        User user = userService.findUserById( userId );
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        return initUser( user, locale );
    }

    private void checkAuth( String auth ) {
        if ( auth == null || auth.length() < 1 || applicationSettings.getIsearch().getAuthTokens() == null
                || !applicationSettings.getIsearch().getAuthTokens().contains( auth ) ) {
            SecurityContextHolder.getContext().setAuthentication( null );
        } else if ( applicationSettings.getIsearch().getAuthTokens().contains( auth ) ) {
            User u = userService.getRemoteAdmin();
            if ( u == null ) {
                log.error( messageSource.getMessage( "ApiController.misconfiguredRemoteAdmin", null, Locale.getDefault() ) );
                return;
            }
            UserPrinciple principle = new UserPrinciple( u );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() ) );
        }
    }

    private Collection<UserGene> initGeneUsers( Collection<UserGene> genes, Locale locale ) {
        for ( UserGene gene : genes ) {
            //noinspection ResultOfMethodCallIgnored // Initializing for the json serializer.
            initUser( gene.getUser(), locale );
            gene.getUser().setUserGenes( new HashMap<>() );
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
        user.setOriginUrl( siteSettings.getFullUrl() );
        return user;
    }

    /**
     * We do not want to query TIER3 genes internationally, so if such request arrives, we have to either
     * try to transform it to only include TIER1&2, or prevent the search.
     *
     * @param tiers the tier type to be restricted to not include tier 3.
     * @return manual (tier1&2) for tier type ANY, or throws an exception if tier type was specifically 3.
     */
    private Set<TierType> restrictTiers( Set<TierType> tiers ) throws TierException {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

}
