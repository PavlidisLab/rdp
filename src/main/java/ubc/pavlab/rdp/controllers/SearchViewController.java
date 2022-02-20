package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@CommonsLog
public class SearchViewController {

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

    @ExceptionHandler({ AccessDeniedException.class })
    public ModelAndView handleAccessDeniedForViewTemplates( HttpServletRequest req, AccessDeniedException exception ) {
        log.warn( "Unauthorized access to the search view via " + req.getRequestURI() + ".", exception );
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setStatus( HttpStatus.UNAUTHORIZED );
        modelAndView.setViewName( "fragments/error::message" );
        modelAndView.addObject( "errorMessage", exception.getMessage() );
        return modelAndView;
    }

    /**
     * Usually, we would have this handled by a 404 error page, but in the case of this endpoint, it will break the
     * client expecting a partial HTML fragment.
     */
    @RequestMapping("/search/view/*")
    public ModelAndView handleMissingRoute() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setStatus( HttpStatus.NOT_FOUND );
        modelAndView.setViewName( "fragments/error::message" );
        modelAndView.addObject( "errorMessage", "No endpoint found for your request URL." );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike,
                                                  @RequestParam Boolean prefix,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds ) );
        modelAndView.addObject( "remote", Boolean.TRUE );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "nameLike" })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike,
                                               @RequestParam Boolean prefix,
                                               @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                               @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                               @RequestParam(required = false) Set<String> organUberonIds ) {
        Collection<User> users;
        if ( prefix ) {
            users = userService.findByStartsName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            users = userService.findByLikeName( nameLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", users );
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike,
                                                      @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                      @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                      @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        return modelAndView;
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
                                               Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::usergenes-table" );

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );

        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoOrthologTaxonId", new String[]{ orthologTaxonId.toString() }, locale ) );
            return modelAndView;
        }

        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search/view/orthologs")
    public ModelAndView searchOrthologsForGene( @RequestParam String symbol,
                                                @RequestParam Integer taxonId,
                                                @RequestParam(required = false) Set<TierType> tiers,
                                                @RequestParam(required = false) Integer orthologTaxonId,
                                                @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                @RequestParam(required = false) Set<String> organUberonIds,
                                                Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/ortholog-table::ortholog-table" );

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        Collection<GeneInfo> orthologs = gene.getOrthologs().stream()
                .filter( g -> orthologTaxon == null || g.getTaxon().equals( orthologTaxon ) )
                .collect( Collectors.toSet() );

        // Check if there is an ortholog request for a different taxon than the original gene
        if ( orthologTaxon != null && !orthologTaxon.equals( gene.getTaxon() ) && orthologs.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Map<Taxon, Set<GeneInfo>> orthologMap = orthologs.stream()
                .sorted( Comparator.comparing( GeneInfo::getTaxon, Taxon.getComparator() ) )
                .collect( Collectors.groupingBy( GeneInfo::getTaxon, LinkedHashMap::new, Collectors.toSet() ) );

        modelAndView.addObject( "orthologs", orthologMap );

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike,
                                                         @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                         @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                         @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike, researcherPositions, researcherCategories, organUberonIds ) );
        modelAndView.addObject( "remote", Boolean.TRUE );
        return modelAndView;
    }


    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "symbol", "taxonId", "orthologTaxonId" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol,
                                                  @RequestParam Integer taxonId,
                                                  @RequestParam(required = false) Set<TierType> tiers,
                                                  @RequestParam(required = false) Integer orthologTaxonId,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Collection<UserGene> userGenes = remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds );

        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::usergenes-table" );
        modelAndView.addObject( "usergenes", userGenes );
        modelAndView.addObject( "remote", Boolean.TRUE );

        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/search/view/user-preview/{userId}")
    public ModelAndView previewUser( @PathVariable Integer userId,
                                     @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/profile::user-preview" );

        User user;
        if ( remoteHost != null ) {
            try {
                user = remoteResourceService.getRemoteUser( userId, URI.create( remoteHost ) );
            } catch ( RemoteException e ) {
                log.error( "Could not retrieve user {0} from {1}.", e );
                modelAndView.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
                modelAndView.setViewName( "fragments/error::message" );
                modelAndView.addObject( "errorMessage", "Error querying remote user." );
                return modelAndView;
            }
        } else {
            user = userService.findUserById( userId );
        }
        if ( user == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage", "Could not find user by given identifier." );
        } else if ( userPreviewIsEmpty( user ) ) {
            modelAndView.setStatus( HttpStatus.NO_CONTENT );
            modelAndView.addObject( "user", user );
        } else {
            modelAndView.addObject( "user", user );
        }
        return modelAndView;
    }

    @PreAuthorize("hasPermission(null, #remoteHost == null ? 'search' : 'international-search')")
    @GetMapping(value = "/search/view/user-preview/by-anonymous-id/{anonymousId}")
    public ModelAndView previewAnonymousUser( @PathVariable UUID anonymousId,
                                              @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/profile::user-preview" );
        User user;
        if ( remoteHost != null ) {
            URI remoteHostUri = URI.create( remoteHost );
            try {
                user = remoteResourceService.getAnonymizedUser( anonymousId, remoteHostUri );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Failed to retrieve anonymized user {} from {}.", anonymousId, remoteHostUri.getAuthority() ), e );
                modelAndView.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
                modelAndView.setViewName( "fragments/error::message" );
                modelAndView.addObject( "errorMessage", "Error querying remote anonymous user." );
                return modelAndView;
            }
        } else {
            user = userService.anonymizeUser( userService.findUserByAnonymousIdNoAuth( anonymousId ) );
        }
        if ( user == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage", "Could not find user by given identifier." );
        } else if ( userPreviewIsEmpty( user ) ) {
            modelAndView.setStatus( HttpStatus.NO_CONTENT );
            modelAndView.addObject( "user", user );
        } else {
            modelAndView.addObject( "user", user );
        }
        return modelAndView;
    }

    private boolean userPreviewIsEmpty( User user ) {
        return ( user.getProfile().getDescription() == null || user.getProfile().getDescription().isEmpty() ) &&
                user.getProfile().getResearcherCategories().isEmpty() &&
                user.getUserOrgans().isEmpty();
    }

    private Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }
}
