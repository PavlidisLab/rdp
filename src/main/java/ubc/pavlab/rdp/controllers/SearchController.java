package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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
import ubc.pavlab.rdp.security.Permissions;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.services.RemoteResourceService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.SearchResult;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
@CommonsLog
public class SearchController extends AbstractSearchController {

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

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private OntologyService ontologyService;

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search")
    public ModelAndView search() {
        return new ModelAndView( "search" )
                .addObject( "activeSearchMode", applicationSettings.getSearch().getEnabledSearchModes().stream().findFirst().orElse( null ) )
                .addObject( "chars", userService.getLastNamesFirstChar() )
                .addObject( "user", userService.findCurrentUser() )
                .addObject( "iSearch", applicationSettings.getIsearch().isDefaultOn() )
                .addObject( "ontologyTerms", Collections.emptyList() );
    }

    @PreAuthorize("hasPermission(null, #userSearchParams.isISearch() ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "nameLike", "descriptionLike" })
    public Object searchUsers( @Valid UserSearchParams userSearchParams, BindingResult bindingResult,
                               Locale locale ) {
        if ( userSearchParams.getNameLike().isEmpty() ^ userSearchParams.getDescriptionLike().isEmpty() ) {
            return redirectToSpecificSearch( userSearchParams.getNameLike(), userSearchParams.getDescriptionLike() );
        }
        List<OntologyTermInfo> ontologyTerms;
        if ( userSearchParams.getOntologyTermIds() != null ) {
            ontologyTerms = ontologyService.findAllTermsByIdInMaintainingOrder( userSearchParams.getOntologyTermIds() );
        } else {
            ontologyTerms = Collections.emptyList();
        }
        ModelAndView modelAndView = new ModelAndView( "search" )
                .addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER )
                .addObject( "chars", userService.getLastNamesFirstChar() )
                .addObject( "nameLike", userSearchParams.getNameLike() )
                .addObject( "descriptionLike", userSearchParams.getDescriptionLike() )
                .addObject( "organUberonIds", userSearchParams.getOrganUberonIds() )
                .addObject( "ontologyTerms", ontologyTerms )
                .addObject( "iSearch", userSearchParams.isISearch() );
        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            if ( bindingResult.getGlobalError() != null ) {
                modelAndView.addObject( "message", messageSource.getMessage( bindingResult.getGlobalError(), locale ) );
            } else {
                modelAndView.addObject( "Invalid user search parameters." );
            }
            modelAndView.addObject( "error", Boolean.TRUE );
            modelAndView.addObject( "users", Collections.emptyList() );
            if ( userSearchParams.isISearch() ) {
                modelAndView.addObject( "itlUsers", Collections.emptyList() );
                modelAndView.addObject( "termsAvailabilityByApiUri", Collections.emptyMap() );
            }
        } else {
            modelAndView.addObject( "searchSummary", summarizeUserSearchParams( userSearchParams, locale ) );
            modelAndView.addObject( "users", userService.findByNameAndDescription( userSearchParams.getNameLike(), userSearchParams.isPrefix(), userSearchParams.getDescriptionLike(), userSearchParams.getResearcherPositions(), userSearchParams.getResearcherCategories(), organsFromUberonIds( userSearchParams.getOrganUberonIds() ), ontologyTermsFromIds( userSearchParams.getOntologyTermIds() ) ) );
            if ( userSearchParams.isISearch() ) {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeNameAndDescription( userSearchParams.getNameLike(), userSearchParams.isPrefix(), userSearchParams.getDescriptionLike(), userSearchParams.getResearcherPositions(), userSearchParams.getResearcherCategories(), userSearchParams.getOrganUberonIds(), ontologyTermsFromIds( userSearchParams.getOntologyTermIds() ) ) );
                modelAndView.addObject( "termsAvailabilityByApiUri", getOntologyAvailabilityByApiUri( userSearchParams.getOntologyTermIds() ) );
            }
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "nameLike" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike,
                                           @RequestParam(required = false) boolean prefix,
                                           @RequestParam boolean iSearch,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           @RequestParam(required = false) List<Integer> ontologyTermIds,
                                           Locale locale ) {
        Collection<User> users;
        if ( prefix ) {
            users = userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) );
        } else {
            users = userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) );
        }
        ModelAndView modelAndView = new ModelAndView( "search" )
                .addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER )
                .addObject( "nameLike", nameLike )
                .addObject( "organUberonIds", organUberonIds )
                .addObject( "chars", userService.getLastNamesFirstChar() )
                .addObject( "user", userService.findCurrentUser() )
                .addObject( "iSearch", iSearch );
        if ( ontologyTermIds != null ) {
            List<OntologyTermInfo> ontologyTerms = ontologyService.findAllTermsByIdInMaintainingOrder( ontologyTermIds );
            modelAndView.addObject( "ontologyTerms", ontologyTerms );
        } else {
            modelAndView.addObject( "ontologyTerms", Collections.emptyList() );
        }
        if ( nameLike.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", "Researcher name cannot be empty." );
            modelAndView.addObject( "error", Boolean.TRUE );
            modelAndView.addObject( "users", Collections.emptyList() );
            modelAndView.addObject( "itlUsers", Collections.emptyList() );
            modelAndView.addObject( "termsAvailabilityByApiUri", Collections.emptyMap() );
        } else {
            modelAndView.addObject( "searchSummary", summarizeUserSearchParams( new UserSearchParams( nameLike, prefix, "", iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) );
            modelAndView.addObject( "users", users );
            if ( iSearch ) {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds, null ) );
                if ( ontologyTermIds != null ) {
                    modelAndView.addObject( "termsAvailabilityByApiUri", getOntologyAvailabilityByApiUri( ontologyTermIds ) );
                }
            }
        }

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
                                                  @RequestParam boolean iSearch,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds,
                                                  @RequestParam(required = false) List<Integer> ontologyTermIds,
                                                  Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "search" )
                .addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER )
                .addObject( "descriptionLike", descriptionLike )
                .addObject( "organUberonIds", organUberonIds )
                .addObject( "chars", userService.getLastNamesFirstChar() )
                .addObject( "iSearch", iSearch )
                .addObject( "user", userService.findCurrentUser() );
        if ( ontologyTermIds != null ) {
            List<OntologyTermInfo> ontologyTerms = ontologyService.findAllTermsByIdInMaintainingOrder( ontologyTermIds );
            modelAndView.addObject( "ontologyTerms", ontologyTerms );
        } else {
            modelAndView.addObject( "ontologyTerms", Collections.emptyList() );
        }
        if ( descriptionLike.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", "Research interests cannot be empty." );
            modelAndView.addObject( "error", Boolean.TRUE );
            modelAndView.addObject( "users", Collections.emptyList() );
            modelAndView.addObject( "itlUsers", Collections.emptyList() );
            modelAndView.addObject( "termsAvailabilityByApiUri", Collections.emptyMap() );
        } else {
            modelAndView.addObject( "searchSummary", summarizeUserSearchParams( new UserSearchParams( "", false, descriptionLike, iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) );
            modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
            if ( iSearch ) {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds, ontologyTermsFromIds( ontologyTermIds ) ) );
                if ( ontologyTermIds != null ) {
                    modelAndView.addObject( "termsAvailabilityByApiUri", getOntologyAvailabilityByApiUri( ontologyTermIds ) );
                }
            }
        }

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search", params = { "symbol", "taxonId" })
    public ModelAndView searchUsersByGene( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam boolean iSearch,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           @RequestParam(required = false) List<Integer> ontologyTermIds,
                                           Locale locale ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        ModelAndView modelAndView = new ModelAndView( "search" )
                .addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_GENE )
                .addObject( "chars", userService.getLastNamesFirstChar() )
                .addObject( "symbol", symbol )
                .addObject( "taxonId", taxonId )
                .addObject( "orthologTaxonId", orthologTaxonId )
                .addObject( "tiers", tiers )
                .addObject( "organUberonIds", organUberonIds )
                .addObject( "iSearch", iSearch );

        if ( ontologyTermIds != null ) {
            List<OntologyTermInfo> ontologyTerms = ontologyService.findAllTermsByIdInMaintainingOrder( ontologyTermIds );
            modelAndView.addObject( "ontologyTerms", ontologyTerms );
        } else {
            modelAndView.addObject( "ontologyTerms", Collections.emptyList() );
        }

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoTaxonId", new String[]{ taxonId.toString() }, locale ) );
            modelAndView.addObject( "error", Boolean.TRUE );
            return modelAndView;
        }

        if ( symbol.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", "Gene symbol cannot be empty." );
            modelAndView.addObject( "error", Boolean.TRUE );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", Boolean.TRUE );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", Boolean.TRUE );
            return modelAndView;
        }

        Map<Taxon, Set<GeneInfo>> orthologMap = orthologs.stream()
                .sorted( Comparator.comparing( GeneInfo::getTaxon, Taxon.getComparator() ) )
                .collect( Collectors.groupingBy( GeneInfo::getTaxon, LinkedHashMap::new, Collectors.toSet() ) );

        modelAndView
                .addObject( "orthologs", orthologMap )
                .addObject( "searchSummary", summarizeGeneSearchParams( new GeneSearchParams( gene, tiers, orthologTaxon, iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds ), locale ) )
                .addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ), ontologyTermsFromIds( ontologyTermIds ) ) );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsergenes", remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds, ontologyTermsFromIds( ontologyTermIds ) ) );
            if ( ontologyTermIds != null ) {
                modelAndView.addObject( "termsAvailabilityByApiUri", getOntologyAvailabilityByApiUri( ontologyTermIds ) );
            }
        }

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/search/user/{userId}")
    public ModelAndView getUser( @PathVariable Integer userId,
                                 @RequestParam(required = false) String remoteHost ) {
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() ) {
            URI remoteHostUri = URI.create( remoteHost );
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, remoteHostUri );
            } catch ( UnknownRemoteApiException e ) {
                return new ModelAndView( "fragments/error::message", HttpStatus.BAD_REQUEST )
                        .addObject( "errorMessage", String.format( "Unknown remote API %s.", remoteHostUri.getRawAuthority() ) );
            } catch ( RemoteException e ) {
                log.warn( String.format( "Could not fetch the remote user id %s from %s: %s.", userId, remoteHostUri.getRawAuthority(), ExceptionUtils.getRootCauseMessage( e ) ) );
                return new ModelAndView( "error/503", HttpStatus.SERVICE_UNAVAILABLE );
            }
        } else {
            viewUser = userService.findUserById( userId );
        }

        if ( viewUser == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND );
        } else {
            return new ModelAndView( "search/user" )
                    .addObject( "user", user )
                    .addObject( "viewUser", viewUser )
                    .addObject( "viewOnly", Boolean.TRUE );
        }
    }

    @Deprecated
    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping("/userView/{userId}")
    public String viewUser( @PathVariable Integer userId,
                            @RequestParam(required = false) String remoteHost,
                            RedirectAttributes redirectAttributes ) {
        redirectAttributes.addAttribute( "remoteHost", remoteHost );
        return "redirect:/search/user/" + userId;
    }

    @Data
    public static class RequestAccessForm {
        @NotNull(message = "Reason cannot be blank.")
        @Size(min = 1, message = "Reason cannot be blank.")
        private String reason;
    }

    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public Object requestGeneAccessView( @PathVariable UUID anonymousId,
                                         @RequestParam(required = false) URI remoteHost,
                                         RedirectAttributes redirectAttributes ) {
        if ( remoteHost != null ) {
            try {
                return "redirect:" + remoteResourceService.getRequestGeneAccessUrl( remoteHost, anonymousId );
            } catch ( UnknownRemoteApiException e ) {
                return new ModelAndView( "error/404", HttpStatus.BAD_REQUEST )
                        .addObject( "message", String.format( "Unknown partner API %s.", remoteHost.getRawAuthority() ) );
            }
        }
        UserGene userGene = userService.findUserGeneByAnonymousIdNoAuth( anonymousId );
        if ( userGene == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND );
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( permissionEvaluator.hasPermission( auth, userGene, Permissions.READ ) ) {
            redirectAttributes.addFlashAttribute( "message", "There is no need to request access as you have sufficient permission to see this gene." );
            return "redirect:/search/user/" + userGene.getUser().getId();
        }
        return new ModelAndView( "search/request-access" )
                .addObject( "requestAccessForm", new RequestAccessForm() )
                .addObject( "userGene", userService.anonymizeUserGene( userGene ) );
    }

    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public ModelAndView requestGeneAccess( @PathVariable UUID anonymousId,
                                           @Valid RequestAccessForm requestAccessForm,
                                           BindingResult bindingResult,
                                           RedirectAttributes redirectAttributes ) {
        UserGene userGene = userService.findUserGeneByAnonymousIdNoAuth( anonymousId );
        if ( userGene == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND );
        }

        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "search/request-access", HttpStatus.BAD_REQUEST )
                    .addObject( "userGene", userService.anonymizeUserGene( userGene ) );
        } else {
            userService.sendGeneAccessRequest( userService.findCurrentUser(), userGene, requestAccessForm.getReason() );
            redirectAttributes.addFlashAttribute( "message", "An access request has been sent and will be reviewed." );
            return new ModelAndView( "redirect:/search" );
        }
    }

    /**
     * This endpoint autocomplete ontology terms.
     * <p>
     * Results are unique and ordered as per {@link SearchResult#compareTo(SearchResult)} which first groups results by
     * match type and then by match.
     */
    @ResponseBody
    @GetMapping("/search/ontology-terms/autocomplete")
    public Object autocompleteTerms( @RequestParam String query, @RequestParam(required = false) Integer ontologyId, Locale locale ) {
        if ( ontologyId != null ) {
            Ontology ontology = ontologyService.findById( ontologyId );
            if ( ontology == null || !ontology.isActive() ) {
                return ResponseEntity
                        .status( HttpStatus.NOT_FOUND )
                        .contentType( MediaType.TEXT_PLAIN )
                        .body( String.format( "No ontology with ID %d exists or is active.", ontologyId ) );
            }
            return ontologyService.autocompleteTerms( query, ontology, 20, locale );
        } else {
            return ontologyService.autocompleteTerms( query, 20, locale );
        }
    }
}
