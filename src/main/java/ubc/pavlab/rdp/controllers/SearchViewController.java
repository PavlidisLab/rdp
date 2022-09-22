package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.exception.UnknownRemoteApiException;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.*;

import javax.validation.Valid;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@CommonsLog
public class SearchViewController extends AbstractSearchController {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Autowired
    private GeneInfoService geneInfoService;

    @ExceptionHandler({ AccessDeniedException.class })
    public ModelAndView handleAccessDeniedForViewTemplates( AccessDeniedException exception ) {
        log.warn( "Unauthorized access to the search view.", exception );
        return new ModelAndView( "fragments/error::message", HttpStatus.UNAUTHORIZED )
                .addObject( "errorMessage", exception.getMessage() );
    }

    /**
     * Usually, we would have this handled by a 404 error page, but in the case of this endpoint, it will break the
     * client expecting a partial HTML fragment.
     */
    @GetMapping("/search/view/*")
    public ModelAndView handleMissingRoute() {
        return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                .addObject( "errorMessage", "No endpoint found for your request URL." );
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "nameLike", "descriptionLike" })
    public Object searchItlUsers( @Valid UserSearchParams userSearchParams, BindingResult bindingResult, Locale locale ) {
        if ( userSearchParams.getNameLike().isEmpty() ^ userSearchParams.getDescriptionLike().isEmpty() ) {
            return redirectToSpecificSearch( userSearchParams.getNameLike(), userSearchParams.getDescriptionLike() );
        }
        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", bindingResult.getGlobalError() != null ? messageSource.getMessage( bindingResult.getGlobalError(), locale ) : "Invalid user search parameters." );
        } else {
            return new ModelAndView( "fragments/user-table::user-table" )
                    .addObject( "users", remoteResourceService.findUsersByLikeNameAndDescription( userSearchParams.getNameLike(), userSearchParams.isPrefix(), userSearchParams.getDescriptionLike(), userSearchParams.getResearcherPositions(), userSearchParams.getResearcherCategories(), userSearchParams.getOrganUberonIds(), ontologyTermsFromIds( userSearchParams.getOntologyTermIds() ) ) )
                    .addObject( "remote", Boolean.TRUE );
        }
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike,
                                                  @RequestParam(required = false) boolean prefix,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds,
                                                  @RequestParam(required = false) List<Integer> ontologyTermIds ) {
        if ( nameLike.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Researcher name cannot be empty." );
        } else {
            return new ModelAndView( "fragments/user-table::user-table" )
                    .addObject( "remote", Boolean.TRUE )
                    .addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds, ontologyTermsFromIds( ontologyTermIds ) ) );
        }
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike,
                                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                         @RequestParam(required = false) Set<String> organUberonIds,
                                                         @RequestParam(required = false) List<Integer> ontologyTermIds ) {
        if ( descriptionLike.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Research interests cannot be empty." );
        } else {
            return new ModelAndView( "fragments/user-table::user-table" )
                    .addObject( "remote", Boolean.TRUE )
                    .addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds, ontologyTermsFromIds( ontologyTermIds ) ) );
        }
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "nameLike", "descriptionLike" })
    public Object searchUsersView( @Valid UserSearchParams userSearchParams, BindingResult bindingResult,
                                   @RequestParam(required = false) boolean summarize,
                                   Locale locale ) {
        if ( userSearchParams.getNameLike().isEmpty() ^ userSearchParams.getDescriptionLike().isEmpty() ) {
            return redirectToSpecificSearch( userSearchParams.getNameLike(), userSearchParams.getDescriptionLike() );
        }
        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", bindingResult.getGlobalError() != null ? messageSource.getMessage( bindingResult.getGlobalError(), locale ) : "Invalid user search parameters." );
        } else if ( summarize ) {
            return new ModelAndView( "fragments/search::summary" )
                    .addObject( "searchSummary", summarizeUserSearchParams( userSearchParams, locale ) );
        } else {
            return new ModelAndView( "fragments/user-table::user-table" )
                    .addObject( "users", userService.findByNameAndDescription( userSearchParams.getNameLike(), userSearchParams.isPrefix(), userSearchParams.getDescriptionLike(), userSearchParams.getResearcherPositions(), userSearchParams.getResearcherCategories(), organsFromUberonIds( userSearchParams.getOrganUberonIds() ), ontologyTermsFromIds( userSearchParams.getOntologyTermIds() ) ) );
        }
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "nameLike" })
    public Object searchUsersByNameView( @RequestParam String nameLike,
                                         @RequestParam(required = false) boolean prefix,
                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                         @RequestParam(required = false) Set<String> organUberonIds,
                                         @RequestParam(required = false) List<Integer> ontologyTermIds,
                                         @RequestParam(required = false) boolean summarize,
                                         Locale locale ) {
        if ( nameLike.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Researcher name cannot be empty." );
        } else {
            ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
            if ( summarize ) {
                return new ModelAndView( "fragments/search::summary" )
                        .addObject( "searchSummary", summarizeUserSearchParams( new UserSearchParams( nameLike, prefix, "", false, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) );
            } else if ( prefix ) {
                modelAndView.addObject( "users", userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
            } else {
                modelAndView.addObject( "users", userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
            }
            return modelAndView;
        }
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "descriptionLike" })
    public Object searchUsersByDescriptionView( @RequestParam String descriptionLike,
                                                @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                @RequestParam(required = false) Set<String> organUberonIds,
                                                @RequestParam(required = false) List<Integer> ontologyTermIds,
                                                @RequestParam(required = false) boolean summarize,
                                                Locale locale ) {
        if ( descriptionLike.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Research interests cannot be empty." );
        } else if ( summarize ) {
            return new ModelAndView( "fragments/search::summary" )
                    .addObject( "searchSummary", summarizeUserSearchParams( new UserSearchParams( "", false, descriptionLike, false, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) );
        } else {
            return new ModelAndView( "fragments/user-table::user-table" )
                    .addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
        }
    }


    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view")
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol,
                                               @RequestParam Integer taxonId,
                                               @RequestParam(required = false) Set<TierType> tiers,
                                               @RequestParam(required = false) Integer orthologTaxonId,
                                               @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                               @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                               @RequestParam(required = false) Set<String> organUberonIds,
                                               @RequestParam(required = false) List<Integer> ontologyTermIds,
                                               @RequestParam(required = false) boolean summarize,
                                               Locale locale ) {
        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
        }

        if ( symbol.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Gene symbol cannot be empty." );
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage",
                            messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoOrthologTaxonId", new String[]{ orthologTaxonId.toString() }, locale ) );
        }

        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
        }

        if ( summarize ) {
            return new ModelAndView( "fragments/search::summary" )
                    .addObject( "searchSummary", summarizeGeneSearchParams( new GeneSearchParams( gene, tiers, orthologTaxon, false, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) );
        } else {
            return new ModelAndView( "fragments/user-table::usergenes-table" )
                    .addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
        }
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view/orthologs")
    public ModelAndView searchOrthologsForGene( @RequestParam String symbol,
                                                @RequestParam Integer taxonId,
                                                @RequestParam(required = false) Set<TierType> tiers,
                                                @RequestParam(required = false) Integer orthologTaxonId,
                                                Locale locale ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
        }

        if ( symbol.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Gene symbol cannot be empty." );
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage",
                            messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
        }

        Map<Taxon, Set<GeneInfo>> orthologMap = orthologs.stream()
                .sorted( Comparator.comparing( GeneInfo::getTaxon, Taxon.getComparator() ) )
                .collect( Collectors.groupingBy( GeneInfo::getTaxon, LinkedHashMap::new, Collectors.toSet() ) );

        return new ModelAndView( "fragments/ortholog-table::ortholog-table" )
                .addObject( "orthologs", orthologMap );
    }

    /**
     * Summarize the available terms by partner API given a list of locally available terms used for searching.
     * <p>
     * The terms from this registry are matched in partner registries using the ontology name and the term ID as per
     * {@link Ontology#getName()} and {@link OntologyTermInfo#getTermId()}.
     *
     * @param ontologyTermIds terms from this registry referred by their IDs
     */
    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping("/search/view/international/available-terms-by-partner")
    public ModelAndView getAvailableTermsByPartner( @RequestParam(required = false) List<Integer> ontologyTermIds ) {
        if ( ontologyTermIds == null || ontologyTermIds.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "There must be at least one specified ontology term ID via 'ontologyTermIds'." );
        }
        return new ModelAndView( "fragments/search::available-terms-by-partner" )
                .addObject( "ontologyAvailabilityByApiUri", getOntologyAvailabilityByApiUri( ontologyTermIds ) );
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "symbol", "taxonId" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol,
                                                  @RequestParam Integer taxonId,
                                                  @RequestParam(required = false) Set<TierType> tiers,
                                                  @RequestParam(required = false) Integer orthologTaxonId,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds,
                                                  @RequestParam(required = false) List<Integer> ontologyTermIds ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        if ( symbol.isEmpty() ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                    .addObject( "errorMessage", "Gene symbol cannot be empty." );
        }

        Taxon taxon = taxonService.findById( taxonId );
        Collection<UserGene> userGenes = remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, ontologyTermsFromIds( ontologyTermIds ) );

        return new ModelAndView( "fragments/user-table::usergenes-table" )
                .addObject( "usergenes", userGenes )
                .addObject( "remote", Boolean.TRUE );
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/search/view/user-preview/{userId}")
    public ModelAndView previewUser( @PathVariable Integer userId,
                                     @RequestParam(required = false) String remoteHost ) {
        User user;
        if ( remoteHost != null ) {
            URI remoteHostUri = URI.create( remoteHost );
            try {
                user = remoteResourceService.getRemoteUser( userId, remoteHostUri );
            } catch ( UnknownRemoteApiException e ) {
                return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                        .addObject( "errorMessage", String.format( "Unknown remote API %s.", remoteHostUri.getRawAuthority() ) );
            } catch ( RemoteException e ) {
                log.warn( String.format( "Failed to retrieve user with ID %d from %s: %s", userId, remoteHostUri.getRawAuthority(), ExceptionUtils.getRootCause( e ) ) );
                return new ModelAndView( "fragments/error::message", HttpStatus.INTERNAL_SERVER_ERROR )
                        .addObject( "errorMessage", "Error querying remote user." );
            }
        } else {
            user = userService.findUserById( userId );
        }
        return previewUserModelAndView( user );
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/search/view/user-preview/by-anonymous-id/{anonymousId}")
    public ModelAndView previewAnonymousUser( @PathVariable UUID anonymousId,
                                              @RequestParam(required = false) String remoteHost ) {
        User user;
        if ( remoteHost != null ) {
            URI remoteHostUri = URI.create( remoteHost );
            try {
                user = remoteResourceService.getAnonymizedUser( anonymousId, remoteHostUri );
            } catch ( UnknownRemoteApiException e ) {
                return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                        .addObject( "errorMessage", String.format( "Unknown remote API %s.", remoteHostUri.getRawAuthority() ) );
            } catch ( RemoteException e ) {
                log.warn( String.format( "Failed to retrieve user with anonymized user ID %s from %s: %s", anonymousId, remoteHostUri.getRawAuthority(), ExceptionUtils.getRootCauseMessage( e ) ) );
                return new ModelAndView( "fragments/error::message", HttpStatus.INTERNAL_SERVER_ERROR )
                        .addObject( "errorMessage", "Error querying remote anonymous user." );
            }
        } else {
            user = userService.anonymizeUser( userService.findUserByAnonymousIdNoAuth( anonymousId ) );
        }
        return previewUserModelAndView( user );
    }

    private static ModelAndView previewUserModelAndView( User user ) {
        if ( user == null ) {
            return new ModelAndView( "fragments/error::message", HttpStatus.NOT_FOUND )
                    .addObject( "errorMessage", "Could not find user by given identifier." );
        } else if ( userPreviewIsEmpty( user ) ) {
            return new ModelAndView( "fragments/profile::user-preview", HttpStatus.NO_CONTENT )
                    .addObject( "user", user );
        } else {
            return new ModelAndView( "fragments/profile::user-preview" )
                    .addObject( "user", user );
        }
    }

    private static boolean userPreviewIsEmpty( User user ) {
        return ( user.getProfile().getDescription() == null || user.getProfile().getDescription().isEmpty() ) &&
                user.getProfile().getResearcherCategories().isEmpty() &&
                user.getUserOrgans().isEmpty();
    }
}
