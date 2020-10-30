package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

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
    MessageSource messageSource;

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    UserOrganService userOrganService;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Autowired
    private PrivacyService privacyService;

    @Autowired
    GeneInfoService geneInfoService;

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = "/search")
    public ModelAndView search() {
        ModelAndView modelAndView = new ModelAndView( "search" );
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

    @PreAuthorize("hasPermission(null, 'international-search')")
    @GetMapping(value = "/search/view/international", params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike,
                                                  @RequestParam Boolean prefix,
                                                  @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                  @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix, researcherPositions, researcherCategories, organUberonIds ) );
        modelAndView.addObject( "remote", true );
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
    @GetMapping(value = "/search/view", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike,
                                                      @RequestParam(required = false) Set<ResearcherPosition> researcherPositions,
                                                      @RequestParam(required = false) Set<ResearcherCategory> researcherCategories,
                                                      @RequestParam(required = false) Set<String> organUberonIds ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/user-table::user-table" );
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
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
        modelAndView.addObject( "remote", true );
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
            modelAndView.addObject( "error", true );
            return modelAndView;
        }

        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }

        Collection<UserGene> orthologs;
        if ( orthologTaxonId == null ) {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndUserOrgansIn( gene, tiers, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( gene, tiers, taxonService.findById( orthologTaxonId ), researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }
        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) ) &&
                        // Check if we got some ortholog results
                        ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }

        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) ) );
        if ( iSearch ) {
            modelAndView.addObject( "itlUsergenes", remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, researcherPositions, researcherCategories, organUberonIds ) );
        }

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
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol, taxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage", messageSource.getMessage( "SearchController.errorNoOrthologTaxonId", new String[]{ orthologTaxonId.toString() }, locale ) );
            return modelAndView;
        }

        Collection<UserGene> orthologs;
        if ( orthologTaxonId == null ) {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndUserOrgansIn( gene, tiers, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( gene, tiers, taxonService.findById( orthologTaxonId ), researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
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

        Collection<UserGene> orthologs;
        if ( orthologTaxonId == null ) {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndUserOrgansIn( gene, tiers, researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        } else {
            orthologs = userGeneService.findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( gene, tiers, taxonService.findById( orthologTaxonId ), researcherPositions, researcherCategories, organsFromUberonIds( organUberonIds ) );
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.setViewName( "fragments/error::message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ symbol, orthologTaxon.getScientificName() }, locale ) );
            return modelAndView;
        }

        Map<Taxon, Set<GeneInfo>> orthologMap = orthologs.stream()
                .map( UserGene::getGeneInfo )
                .filter( Objects::nonNull )
                .collect( Collectors.groupingBy( GeneInfo::getTaxon, Collectors.toSet() ) );

        modelAndView.addObject( "orthologs", orthologMap );

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
        modelAndView.addObject( "remote", true );

        return modelAndView;
    }

    @GetMapping(value = "/search/view/user-preview/{userId}")
    public ModelAndView previewUser( @PathVariable Integer userId,
                                     @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/profile::user-preview" );
        modelAndView.addObject( "user", userService.findUserById( userId ) );
        return modelAndView;
    }

    @GetMapping(value = "/userView/{userId}")
    public ModelAndView viewUser( @PathVariable Integer userId,
                                  @RequestParam(required = false) String remoteHost ) {
        ModelAndView modelAndView = new ModelAndView( "userView" );
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() && privacyService.checkCurrentUserCanSearch( true ) ) {
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, URI.create( remoteHost ) );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Could not fetch the remote user id {0} from {1}.", userId, remoteHost ), e );
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
            modelAndView.addObject( "viewOnly", true );
        }
        return modelAndView;
    }

    private Collection<UserOrgan> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : userOrganService.findByUberonIdIn( organUberonIds );
    }

}
