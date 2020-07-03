package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import java.util.*;

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
    private RemoteResourceService remoteResourceService;

    @Autowired
    private PrivacyService privacyService;

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = { "nameLike", "iSearch" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike, @RequestParam Boolean iSearch, @RequestParam Boolean prefix ) {
        User user = userService.findCurrentUser();
        if(!privacyService.checkUserCanSearch( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET, params = { "nameLike", })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike, @RequestParam Boolean prefix ) {
        if(!privacyService.checkCurrentUserCanSearch( false )){
            return null;
        }
        Collection<User> users = prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike );
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", users );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike, @RequestParam Boolean prefix ) {
        if(!privacyService.checkCurrentUserCanSearch(  true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = { "descriptionLike", "iSearch" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
            @RequestParam Boolean iSearch ) {
        User user = userService.findCurrentUser();
        if(!privacyService.checkUserCanSearch( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        // FIXME: this query should not happen or should be optimized
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET, params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike ) {
        if(!privacyService.checkCurrentUserCanSearch( true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = {"symbol", "taxonId"})
    public ModelAndView searchUsersByGene( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId,
                                           Locale locale ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        if (tiers == null) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, taxonService.findById( orthologTaxonId ) );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "search" );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[] {symbol}, locale));
        } else if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ orthologTaxonId.toString() }, locale ) );
        } else {
            modelAndView.addObject( "usergenes", userGeneService.handleGeneSearchWithoutSecurityFilter( gene, tiers, orthologs ) );
            if ( iSearch ) {
                try {
                    modelAndView.addObject( "itlUsergenes",
                            remoteResourceService.findGenesBySymbol (symbol, taxon, tiers, orthologTaxonId) );
                } catch ( RemoteException e ) {
                    modelAndView.addObject( "itlErrorMessage", e.getMessage() );
                }
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET)
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol,
                                               @RequestParam Integer taxonId,
                                               @RequestParam(required = false) Set<TierType> tiers,
                                               @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId,
                                               Locale locale ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );

        if (taxon == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if (gene == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[] {symbol}, locale));
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologTaxon", new String[] {orthologTaxonId.toString()}, locale));
            return modelAndView;
        }

        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, orthologTaxon );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[] {symbol}, locale));
            return modelAndView;
        }

        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologs ) );
        modelAndView.setViewName( "fragments/user-table :: usergenes-table" );

        return modelAndView;
    }

    @Autowired
    GeneInfoService geneInfoService;

    @RequestMapping(value = "/search/view/orthologs", method = RequestMethod.GET)
    public ModelAndView searchOrthologsForGene(@RequestParam String symbol, @RequestParam Integer taxonId,
					       @RequestParam(required = false) Set<TierType> tiers,
					       @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId, Locale locale ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if (tiers == null) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        if (taxon == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol }, locale ) );
            return modelAndView;
        }

        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, taxonService.findById(orthologTaxonId) );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[] {symbol}, locale));
            return modelAndView;
        }

        Map<String, List<Gene>> orthologMap = new HashMap<>();
        for (Gene o : orthologs){
            String name = o.getTaxon().getCommonName();
            if (!orthologMap.containsKey(name)) {
                orthologMap.put(name, new ArrayList<Gene>());
            }
            orthologMap.get(name).add(o);
        }

        modelAndView.addObject( "orthologs", orthologMap );
        modelAndView.setViewName( "fragments/ortholog-table :: ortholog-table" );

        return modelAndView;
    }



    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "symbol", "taxonId", "tiers" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam Set<TierType> tiers,
            @RequestParam(name = "orthologTaxonId", required = false) final Integer orthologTaxonId ) {
        if(!privacyService.checkCurrentUserCanSearch( true )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            // FIXME: orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );
        ModelAndView modelAndView = new ModelAndView();

        try {
            Collection<UserGene> userGenes = remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId);
            modelAndView.addObject( "usergenes", userGenes);
            modelAndView.addObject( "remote", true );
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/userView/{userId}", method = RequestMethod.GET)
    public ModelAndView viewUser( @PathVariable Integer userId,
            @RequestParam(name = "remoteHost", required = false) String remoteHost ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() && privacyService.checkCurrentUserCanSearch( true ) ) {
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, remoteHost );
            } catch ( RemoteException e ) {
                log.error( "Could not fetch the remote user id " + userId + " from " + remoteHost );
                e.printStackTrace();
                return null;
            }
        } else {
            viewUser = userService.findUserById( userId );
        }

        if ( viewUser == null ) {
            modelAndView.setViewName( "error/404" );
        } else {
            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", true );
            modelAndView.setViewName( "userView" );
        }
        return modelAndView;
    }

}
