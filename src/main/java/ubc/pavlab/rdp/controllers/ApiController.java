package ubc.pavlab.rdp.controllers;

import io.swagger.v3.oas.annotations.Operation;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.exception.ApiException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.*;
import ubc.pavlab.rdp.security.Permissions;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.net.URI;
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
    @Autowired
    private UserPrivacyService userPrivacyService;
    @Autowired
    private BuildProperties buildProperties;

    @ExceptionHandler({ AccessDeniedException.class })
    public ResponseEntity<?> handleAccessDeniedException( Exception e ) {
        // the stacktrace will contain enough information to trace the error, so no need to log the request URI
        log.warn( "Unauthorized access to the API.", e );
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
    @Operation(hidden = true)
    @GetMapping(value = "/api/*")
    public void handleMissingRoute() {
        throw new ApiException( HttpStatus.NOT_FOUND, "No endpoint found for your request URL." );
    }

    /**
     * Provide general statistics about this registry.
     */
    @GetMapping(value = "/api/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public Stats getStats() {
        return Stats.builder()
                .version( buildProperties.getVersion() )
                .users( userService.countResearchers() )
                .publicUsers( userService.countPublicResearchers() )
                .usersWithGenes( userGeneService.countUsersWithGenes() )
                .userGenes( userGeneService.countAssociations() )
                .uniqueUserGenes( userGeneService.countUniqueAssociations() )
                .uniqueUserGenesInAllTiers( userGeneService.countUniqueAssociationsAllTiers() )
                .uniqueHumanUserGenesInAllTiers( userGeneService.countUniqueAssociationsToHumanAllTiers() )
                .researchersByTaxonId( userGeneService.researcherCountByTaxonId() )
                .build();
    }

    @GetMapping("/api/taxa/{taxonId}")
    public Taxon getTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( "No taxon with ID %d.", taxonId ) );
        }
        return taxon;
    }

    /**
     * Retrieve all users in a paginated format.
     * <p>
     * Results that cannot be displayed are anonymized.
     */
    @GetMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<User> getUsers( Pageable pageable,
                                Locale locale ) {
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
            return userService.findByEnabledTrueNoAuth( pageable )
                    .map( user -> permissionEvaluator.hasPermission( auth2, user, Permissions.READ ) ? user : userService.anonymizeUser( user ) )
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
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            final Authentication auth2 = SecurityContextHolder.getContext().getAuthentication();
            return userGeneService.findByUserEnabledTrueNoAuth( pageable )
                    .map( userGene -> permissionEvaluator.hasPermission( auth2, userGene, Permissions.READ ) ? userGene : userService.anonymizeUserGene( userGene ) )
                    .map( userGene -> initUserGene( userGene, locale ) );
        } else {
            return userGeneService.findByUserEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType.PUBLIC, pageable ).map( userGene -> initUserGene( userGene, locale ) );
        }
    }

    @GetMapping(value = "/api/ontologies", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RemoteOntology> getOntologies( Locale locale ) {
        return ontologyService.findAllOntologies().stream()
                .map( o -> initOntology( o, locale ) )
                .collect( Collectors.toList() );
    }

    @GetMapping(value = "/api/ontologies/{ontologyName}/terms", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<RemoteOntologyTermInfo> getOntologyTerms( @PathVariable String ontologyName, Pageable pageable, Locale locale ) {
        Ontology ontology = ontologyService.findByName( ontologyName );
        if ( ontology == null || !ontology.isActive() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "No ontology %s.", ontologyName ) );
        }
        return ontologyService.findAllTermsByOntology( ontology, pageable )
                .map( t -> initTermInfo( t, locale ) );
    }

    @GetMapping(value = "/api/ontologies/{ontologyName}/terms", params = { "ontologyTermIds" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RemoteOntologyTermInfo> getOntologyTermsByOntologyNameAndTermIds( @PathVariable String ontologyName, @RequestParam List<String> ontologyTermIds, Locale locale ) {
        Ontology ontology = ontologyService.findByName( ontologyName );
        if ( ontology == null || !ontology.isActive() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "No ontology %s.", ontologyName ) );
        }
        if ( ontologyTermIds == null || ontologyTermIds.isEmpty() ) {
            return Collections.emptyList(); // technically impossible since that would match getOntologyTerms()
        }
        return ontologyService.findAllTermsByOntologyAndTermIdIn( ontology, ontologyTermIds ).stream()
                .map( t -> initTermInfo( t, locale ) )
                .collect( Collectors.toList() );
    }

    @GetMapping(value = "/api/ontologies/{ontologyName}/terms/{termId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public OntologyTermInfo getOntologyTerm( @PathVariable String ontologyName, @PathVariable String termId, Locale locale ) {
        OntologyTermInfo ontologyTermInfo = ontologyService.findTermByTermIdAndOntologyName( termId, ontologyName );
        if ( ontologyTermInfo == null || !ontologyTermInfo.isActive() || !ontologyTermInfo.getOntology().isActive() ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "No ontology term %s in ontology %s.", termId, ontologyName ) );
        }
        return initTermInfo( ontologyTermInfo, locale );
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> searchUsersByName( @RequestParam String nameLike,
                                         @RequestParam Boolean prefix,
                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                         @RequestParam(required = false) Set<String> organUberonIds,
                                         @RequestParam(required = false) List<String> ontologyNames,
                                         @RequestParam(required = false) List<String> ontologyTermIds,
                                         Locale locale ) {
        if ( prefix ) {
            return initUsers( userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
        } else {
            return initUsers( userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
        }
    }

    @GetMapping(value = "/api/users/search", params = { "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> searchUsersByDescription( @RequestParam String descriptionLike,
                                                @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                @RequestParam(required = false) Set<String> organUberonIds,
                                                @RequestParam(required = false) List<String> ontologyNames,
                                                @RequestParam(required = false) List<String> ontologyTermIds,
                                                Locale locale ) {
        return initUsers( userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
    }

    @GetMapping(value = "/api/users/search", params = { "nameLike", "descriptionLike" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<User> searchUsersByNameAndDescription( @RequestParam String nameLike,
                                                       @RequestParam(required = false) boolean prefix,
                                                       @RequestParam String descriptionLike,
                                                       @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                       @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                       @RequestParam(required = false) Set<String> organUberonIds,
                                                       @RequestParam(required = false) List<String> ontologyNames,
                                                       @RequestParam(required = false) List<String> ontologyTermIds,
                                                       Locale locale ) {
        return initUsers( userService.findByNameAndDescription( nameLike, prefix, descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromOntologyWithTermIds( ontologyNames, ontologyTermIds ) ), locale );
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
                                                   @RequestParam(required = false) List<String> ontologyNames,
                                                   @RequestParam(required = false) List<String> ontologyTermIds,
                                                   Locale locale ) {

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown taxon ID: %s.", taxonId ) );
        }

        if ( symbol.isEmpty() ) {
            throw new ApiException( HttpStatus.BAD_REQUEST, "Gene symbol cannot be empty." );
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
    public List<UserGene> searchUsersByGeneSymbol( @RequestParam String symbol,
                                                   @RequestParam Integer taxonId,
                                                   @RequestParam String tier,
                                                   @RequestParam(required = false) Integer orthologTaxonId,
                                                   @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                   @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                   @RequestParam(required = false) Set<String> organUberonIds,
                                                   @RequestParam(required = false) List<String> ontologyNames,
                                                   @RequestParam(required = false) List<String> ontologyTermIds,
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

        return searchUsersByGeneSymbol( symbol, taxonId, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, ontologyNames, ontologyTermIds, locale );
    }

    @GetMapping(value = "/api/users/{userId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public User getUserById( @PathVariable Integer userId,
                             Locale locale ) {
        User user = userService.findUserById( userId );
        if ( user == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( locale, "Unknown user with ID: %d.", userId ) );
        }
        return initUser( user, locale );
    }

    @GetMapping(value = "/api/users/by-anonymous-id/{anonymousId}")
    public User getUserByAnonymousId( @PathVariable UUID anonymousId,
                                      Locale locale ) {
        User user = userService.findUserByAnonymousIdNoAuth( anonymousId );
        if ( user == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( "Unknown user with anonymous ID: %s.", anonymousId ) );
        }
        if ( permissionEvaluator.hasPermission( SecurityContextHolder.getContext().getAuthentication(), user, Permissions.READ ) ) {
            return initUser( user, locale );
        } else {
            return initUser( userService.anonymizeUser( user, anonymousId ), locale );
        }
    }

    @GetMapping(value = "/api/genes/by-anonymous-id/{anonymousId}")
    public UserGene getUserGeneByAnonymousId( @PathVariable UUID anonymousId,
                                              Locale locale ) {
        UserGene userGene = userService.findUserGeneByAnonymousIdNoAuth( anonymousId );
        if ( userGene == null ) {
            throw new ApiException( HttpStatus.NOT_FOUND, String.format( "Unknown gene with anonymous ID: %s.", anonymousId ) );
        }
        if ( permissionEvaluator.hasPermission( SecurityContextHolder.getContext().getAuthentication(), userGene, Permissions.READ ) ) {
            return initUserGene( userGene, locale );
        } else {
            return initUserGene( userService.anonymizeUserGene( userGene, anonymousId ), locale );
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
        if ( !userPrivacyService.checkCurrentUserCanSeeGeneList( user ) ) {
            user.getUserGenes().clear();
        }
        user.getUserOntologyTerms().forEach( term -> initUserTerm( term, locale ) );
        return initRemoteResource( user, locale );
    }

    private RemoteOntology initOntology( Ontology ontology, Locale locale ) {
        RemoteOntology remoteOntology = RemoteOntology.builder( ontology.getName() ).build();
        remoteOntology.setNumberOfTerms( ontologyService.countActiveTerms( ontology ) );
        remoteOntology.setNumberOfObsoleteTerms( ontologyService.countActiveAndObsoleteTerms( ontology ) );
        try {
            remoteOntology.setDefinition( messageSource.getMessage( ontology.getResolvableDefinition(), locale ) );
        } catch ( NoSuchMessageException e ) {
            remoteOntology.setDefinition( null );
        }
        return initRemoteResource( remoteOntology, locale );
    }

    private RemoteOntologyTermInfo initTermInfo( OntologyTermInfo termInfo, Locale locale ) {
        RemoteOntologyTermInfo remoteOntologyTermInfo = RemoteOntologyTermInfo.builder( initOntology( termInfo.getOntology(), locale ), termInfo.getTermId() ).build();
        remoteOntologyTermInfo.setName( messageSource.getMessage( termInfo.getResolvableTitle(), locale ) );
        try {
            remoteOntologyTermInfo.setDefinition( messageSource.getMessage( termInfo.getResolvableDefinition(), locale ) );
        } catch ( NoSuchMessageException e ) {
            remoteOntologyTermInfo.setDefinition( null );
        }
        // TODO: perform this in a single query
        remoteOntologyTermInfo.setSubTermIds( termInfo.getSubTerms().stream()
                .filter( OntologyTermInfo::isActive )
                .map( OntologyTermInfo::getTermId )
                .collect( Collectors.toSet() ) );
        return initRemoteResource( remoteOntologyTermInfo, locale );
    }

    private void initUserTerm( UserOntologyTerm term, Locale locale ) {
        term.setOntology( initOntology( term.getOntology(), locale ) );
        term.setName( messageSource.getMessage( term.getResolvableTitle(), locale ) );
    }

    @SneakyThrows
    private <T extends RemoteResource> T initRemoteResource( T remoteResource, Locale locale ) {
        remoteResource.setOrigin( messageSource.getMessage( "rdp.site.shortname", null, locale ) );
        // Ensure that the path of the URL is effectively stripped from any trailing slashes and that its string
        // representation is free of query parameters, fragments, etc.
        // The main reason we do this is to avoid double slashes when generating URLs to profiles on partner sites
        remoteResource.setOriginUrl( new URI( siteSettings.getHostUrl().getScheme(), siteSettings.getHostUrl().getAuthority(), StringUtils.trimTrailingCharacter( siteSettings.getHostUrl().getPath(), '/' ), null, null ) );
        return remoteResource;
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

    private Map<Ontology, Set<OntologyTermInfo>> ontologyTermsFromOntologyWithTermIds( List<String> ontologyNames, List<String> termIds ) {
        if ( ontologyNames == null || termIds == null ) {
            return null;
        }
        if ( ontologyNames.size() != termIds.size() ) {
            throw new ApiException( HttpStatus.BAD_REQUEST, "The 'ontologyNames' and 'ontologyTermIds' lists must have the same size." );
        }
        Map<Ontology, Set<OntologyTermInfo>> results = ontologyService.findTermByTermIdsAndOntologyNames( termIds, ontologyNames ).stream()
                .collect( Collectors.groupingBy( OntologyTerm::getOntology, Collectors.toSet() ) );
        Set<String> foundOntologyNames = results.keySet().stream().map( Ontology::getName ).collect( Collectors.toSet() );

        // sorted alphabetically
        SortedSet<String> missingOntologyNames = new TreeSet<>( ontologyNames );
        missingOntologyNames.removeAll( foundOntologyNames );

        // attempt to retrieve any missing ontologies (and associate it to an empty collection)
        for ( String missingOntologyName : missingOntologyNames ) {
            Ontology missingOntology = ontologyService.findByNameAndActiveTrue( missingOntologyName );
            if ( missingOntology != null ) {
                results.put( missingOntology, Collections.emptySet() );
                foundOntologyNames.add( missingOntologyName );
            }
        }

        // remove found ontologies without matching terms
        missingOntologyNames.removeAll( foundOntologyNames );

        // all ontologies must be represented
        if ( !missingOntologyNames.isEmpty() ) {
            throw new ApiException( HttpStatus.BAD_REQUEST, String.format( "The following ontologies do not exist in this registry: %s.", String.join( ", ", missingOntologyNames ) ) );
        }

        return results;
    }
}
