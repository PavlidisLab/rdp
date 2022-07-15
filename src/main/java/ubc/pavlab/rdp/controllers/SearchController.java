package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
@CommonsLog
public class SearchController {

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Autowired
    private GeneInfoService geneInfoService;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ApplicationSettings applicationSettings;

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search")
    public ModelAndView search() {
        ModelAndView modelAndView = new ModelAndView( "search" );
        modelAndView.addObject( "activeSearchMode", applicationSettings.getSearch().getEnabledSearchModes().stream().findFirst().orElse( null ) );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "user", userService.findCurrentUser() );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "nameLike" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike,
                                           @RequestParam Boolean prefix,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "search" );
        Collection<User> users;
        if ( prefix ) {
            users = userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            users = userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }
        modelAndView.addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER );
        modelAndView.addObject( "nameLike", nameLike );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "user", userService.findCurrentUser() );
        modelAndView.addObject( "users", users );
        modelAndView.addObject( "iSearch", iSearch );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds ) );
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #iSearch ? 'international-search' : 'search')")
    @GetMapping(value = "/search", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
                                                  @RequestParam Boolean iSearch,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "search" );
        modelAndView.addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_RESEARCHER );
        modelAndView.addObject( "descriptionLike", descriptionLike );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "iSearch", iSearch );

        modelAndView.addObject( "user", userService.findCurrentUser() );

        modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds ) );
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search", params = { "symbol", "taxonId" })
    public ModelAndView searchUsersByGene( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                           @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        ModelAndView modelAndView = new ModelAndView( "search" );

        modelAndView.addObject( "activeSearchMode", ApplicationSettings.SearchSettings.SearchMode.BY_GENE );
        modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
        modelAndView.addObject( "symbol", symbol );
        modelAndView.addObject( "taxonId", taxonId );
        modelAndView.addObject( "orthologTaxonId", orthologTaxonId );
        modelAndView.addObject( "tiers", tiers );
        modelAndView.addObject( "organUberonIds", organUberonIds );
        modelAndView.addObject( "iSearch", iSearch );

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoTaxonId", new String[]{ taxonId.toString() }, locale ) );
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

        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsergenes", remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds ) );
        }

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/userView/{userId}")
    public ModelAndView viewUser( @PathVariable Integer userId,
                                  @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "userView" );
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() ) {
            URI remoteHostUri = URI.create( remoteHost );
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, remoteHostUri );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Could not fetch the remote user id {0} from {1}.", userId, remoteHostUri.getAuthority() ), e );
                modelAndView.setStatus( HttpStatus.SERVICE_UNAVAILABLE );
                modelAndView.setViewName( "error/503" );
                return modelAndView;
            }
        } else {
            viewUser = userService.findUserById( userId );
        }

        if ( viewUser == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
        } else {
            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", Boolean.TRUE );
        }
        return modelAndView;
    }

    @Data
    private static class RequestAccessForm {
        @NotNull(message = "Reason cannot be blank.")
        @Size(min = 1, message = "Reason cannot be blank.")
        private String reason;
    }

    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public Object requestGeneAccessView( @PathVariable UUID anonymousId,
                                         RedirectAttributes redirectAttributes ) {
        ModelAndView modelAndView = new ModelAndView( "search/request-access" );
        UserGene userGene = userService.findUserGeneByAnonymousIdNoAuth( anonymousId );
        if ( userGene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
            return modelAndView;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( permissionEvaluator.hasPermission( auth, userGene, "read" ) ) {
            String redirectUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path( "userView/{userId}" )
                    .buildAndExpand( Collections.singletonMap( "userId", userGene.getUser().getId() ) )
                    .toUriString();
            redirectAttributes.addFlashAttribute( "message", "There is no need to request access as you have sufficient permission to see this gene." );
            return new RedirectView( redirectUri );
        }
        modelAndView.addObject( "requestAccessForm", new RequestAccessForm() );
        modelAndView.addObject( "userGene", userService.anonymizeUserGene( userGene ) );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search') and hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/search/gene/by-anonymous-id/{anonymousId}/request-access")
    public ModelAndView requestGeneAccess( @PathVariable UUID anonymousId,
                                           @Valid RequestAccessForm requestAccessForm,
                                           BindingResult bindingResult,
                                           RedirectAttributes redirectAttributes ) {
        ModelAndView modelAndView = new ModelAndView( "search/request-access" );
        UserGene userGene = userService.findUserGeneByAnonymousIdNoAuth( anonymousId );
        if ( userGene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
            return modelAndView;
        }

        modelAndView.addObject( "userGene", userService.anonymizeUserGene( userGene ) );

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
        } else {
            userService.sendGeneAccessRequest( userService.findCurrentUser(), userGene, requestAccessForm.getReason() );
            redirectAttributes.addFlashAttribute( "message", "An access request has been sent and will be reviewed." );
            return new ModelAndView( "redirect:/search" );
        }
        return modelAndView;
    }

    private Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }
}
