package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
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
                                     @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                     @RequestParam(required = false) Set<String> organUberonIds,
                                     @RequestParam(required = false) String auth,
                                     Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        if ( prefix ) {
            return initUsers( userService.findByStartsName( nameLike, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        } else {
            return initUsers( userService.findByLikeName( nameLike, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByDescription( @RequestParam String descriptionLike,
                                            @RequestParam(required = false) String auth,
                                            @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                            @RequestParam(required = false) Set<String> organUberonIds,
                                            Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        return initUsers( userService.findByDescription( descriptionLike, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
    }

    /**
     * Search for genes by symbol, taxon, tier, orthologs and organ systems.
     */
    @GetMapping(value = "/api/genes/search", params = { "symbol", "taxonId" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByGeneSymbol( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) String auth,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
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

        Collection<UserGene> orthologs;
        if ( orthologTaxon != null ) {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( gene, tiers, orthologTaxon, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndUserOrgansIn( gene, tiers, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            return new ResponseEntity<>( messageSource.getMessage( "ApiController.noOrthologsWithGivenParameters", null, locale ), null, HttpStatus.NOT_FOUND );
        }

        return initGeneUsers( userGeneService.handleGeneSearch( gene, restrictTiers( tiers ), orthologTaxon, researcherCategories, organsFromUberonIds( organUberonIds ) ), locale );
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
                                           @RequestParam(required = false) String auth,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
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

        return searchUsersByGeneSymbol( symbol, taxonId, tiers, auth, orthologTaxonId, researcherCategories, organUberonIds, locale );
    }

    @GetMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
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
        if ( applicationSettings.getIsearch().getAuthTokens().contains( auth ) ) {
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
            // Initializing for the json serializer.
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
    private Set<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

    private Collection<UserOrgan> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : userOrganService.findByUberonIdIn( organUberonIds );
    }

}
