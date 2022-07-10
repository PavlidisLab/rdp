package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
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
    @Autowired
    private OntologyService ontologyService;

    @ExceptionHandler({ AuthenticationException.class, AccessDeniedException.class })
    public ResponseEntity<?> handleAuthenticationExceptionAndAccessDeniedException( HttpServletRequest req, Exception e ) {
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
    public Object getUsers( @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                            @Deprecated @RequestParam(required = false) String auth,
                            Pageable pageable,
                            Locale locale ) {
        checkAuth( authorizationHeader, auth );
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
    public Object getGenes( @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                            @Deprecated @RequestParam(required = false) String auth,
                            Pageable pageable,
                            Locale locale ) {
        checkAuth( authorizationHeader, auth );
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
            return userGeneService.findByUserEnabledTrueNoAuth( pageable )
                    .map( userGene -> permissionEvaluator.hasPermission( auth2, userGene, "read" ) ? userGene : userService.anonymizeUserGene( userGene ) )
                    .map( userGene -> initUserGene( userGene, locale ) );
        } else {
            return userGeneService.findByUserEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType.PUBLIC, pageable ).map( userGene -> initUserGene( userGene, locale ) );
        }
    }

    @GetMapping(value = "/api/ontologies", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Ontology> getOntologies( Locale locale ) {
        return ontologyService.findAllOntologies().stream()
                .map( o -> initOntology( o, locale ) )
                .collect( Collectors.toList() );
    }

    @GetMapping(value = "/api/ontologies/{ontologyName}/terms", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<OntologyTermInfo> getOntologyTerms( @PathVariable String ontologyName, Pageable pageable, Locale locale ) {
        Ontology ontology = ontologyService.findByName( ontologyName );
        if ( ontology == null || !ontology.isActive() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "No ontology %s.", ontologyName ) );
        }
        return ontologyService.findAllTermsByOntology( ontology, pageable )
                .map( t -> initTerm( t, locale ) );
    }

    @GetMapping(value = "/api/ontologies/{ontologyName}/terms/{termId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OntologyTermInfo getOntologyTerm( @PathVariable String ontologyName, @PathVariable String termId, Locale locale ) {
        OntologyTermInfo ontologyTermInfo = ontologyService.findTermByTermIdAndOntologyName( termId, ontologyName );
        if ( ontologyTermInfo == null || !ontologyTermInfo.isActive() || !ontologyTermInfo.getOntology().isActive() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "No ontology term %s in ontology %s.", termId, ontologyName ) );
        }
        return initTerm( ontologyTermInfo, locale );
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByName( @RequestParam String nameLike,
                                     @RequestParam Boolean prefix,
                                     @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                     @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                     @RequestParam(required = false) Set<String> organUberonIds,
                                     @RequestParam(required = false) List<String> ontologyNames,
                                     @RequestParam(required = false) List<String> ontologyTermIds,
                                     @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                     @Deprecated @RequestParam(required = false) String auth,
                                     Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        if ( prefix ) {
            return initUsers( userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
        } else {
            return initUsers( userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public Object searchUsersByDescription( @RequestParam String descriptionLike,
                                            @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                            @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                            @RequestParam(required = false) Set<String> organUberonIds,
                                            @RequestParam(required = false) List<String> ontologyNames,
                                            @RequestParam(required = false) List<String> ontologyTermIds,
                                            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                            @Deprecated @RequestParam(required = false) String auth,
                                            Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );
        return initUsers( userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
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
                                           @RequestParam(required = false) List<String> ontologyNames,
                                           @RequestParam(required = false) List<String> ontologyTermIds,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @Deprecated @RequestParam(required = false) String auth,
                                           Locale locale ) {
        checkEnabled();
        checkAuth( authorizationHeader, auth );

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

        return initUserGenes( userGeneService.handleGeneSearch( gene, restrictTiers( tiers ), orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
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
                                           @RequestParam(required = false) List<String> ontologyNames,
                                           @RequestParam(required = false) List<String> ontologyTermIds,
                                           @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @Deprecated @RequestParam(required = false) String auth,
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

        return searchUsersByGeneSymbol( symbol, taxonId, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, ontologyNames, ontologyTermIds, authorizationHeader, auth, locale );
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
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown user with ID: %d.", userId ) );
        }
        return initUser( user, locale );
    }

    @GetMapping(value = "/api/users/by-anonymous-id/{anonymousId}")
    public Object getUserByAnonymousId( @PathVariable UUID anonymousId,
                                        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                        @RequestParam(name = "auth", required = false) String auth,
                                        Locale locale ) {
        checkEnabled();
        checkAnonymousResultsEnabled();
        checkAuth( authorizationHeader, auth );
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

    private void checkAnonymousResultsEnabled() {
        if ( !applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            throw new ApiException( HttpStatus.SERVICE_UNAVAILABLE, "Anonymized search results is not available for this registry." );
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
            u = userService.getRemoteSearchUser()
                    .orElseThrow( () -> new ApiException( HttpStatus.SERVICE_UNAVAILABLE, messageSource.getMessage( "ApiController.misconfiguredRemoteAdmin", null, Locale.getDefault() ) ) );
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

    private Collection<UserGene> initUserGenes( Collection<UserGene> genes, Locale locale ) {
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

    private Ontology initOntology( Ontology ontology, Locale locale ) {
        ontology.setNumberOfTerms( ontologyService.countActiveTerms( ontology ) );
        ontology.setNumberOfObsoleteTerms( ontologyService.countActiveAndObsoleteTerms( ontology ) );
        ontology.setDefinition( messageSource.getMessage( "rdp.ontologies." + ontology.getName() + ".definition", null, ontology.getDefinition(), locale ) );
        return ontology;
    }

    private OntologyTermInfo initTerm( OntologyTermInfo term, Locale locale ) {
        term.setOntology( initOntology( term.getOntology(), locale ) );
        term.setDefinition( messageSource.getMessage( "rdp.ontologies." + term.getOntology().getName() + ".terms." + term.getTermId() + ".definition", null, term.getDefinition(), locale ) );
        // TODO: perform this in a single query
        term.setSubTermIds( term.getSubTerms().stream()
                .filter( OntologyTermInfo::isActive )
                .map( OntologyTermInfo::getTermId )
                .collect( Collectors.toSet() ) );
        return term;
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

    private Collection<OntologyTermInfo> ontologyTermsFromOntologyWithTermIds( List<String> ontologyNames, List<String> termIds ) {
        if ( ontologyNames == null || termIds == null ) {
            return null;
        }
        if ( ontologyNames.size() != termIds.size() ) {
            throw new ApiException( HttpStatus.BAD_REQUEST, "The 'ontologyNames' and 'ontologyTermIds' lists must have the same size." );
        }
        // TODO: perform this in a single query
        List<OntologyTermInfo> results = new ArrayList<>( ontologyNames.size() );
        for ( int i = 0; i < ontologyNames.size(); i++ ) {
            OntologyTermInfo oti = ontologyService.findTermByTermIdAndOntologyName( termIds.get( i ), ontologyNames.get( i ) );
            if ( oti != null ) {
                results.add( oti );
            }
        }
        if ( results.isEmpty() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, "None of the supplied terms could be found." );
        }
        return results;
    }
}
