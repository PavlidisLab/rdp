package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;

import java.util.Collection;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
public class ManagerController {

    private static Log log = LogFactory.getLog( ManagerController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private UserGeneService userGeneService;

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = {"nameLike"})
    public ModelAndView searchUsersByName( @RequestParam String nameLike ) {
        User user = userService.findCurrentUser();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", userService.findByLikeName( nameLike ) );
//        modelAndView.setViewName( "fragments/user-table :: user-table" );
        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = {"nameLike"})
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByLikeName( nameLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = {"descriptionLike"})
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike ) {
        User user = userService.findCurrentUser();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = {"descriptionLike"})
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = {"symbolLike", "taxonId", "tier"})
    public ModelAndView searchUsersByGeneSymbol( @RequestParam String symbolLike, @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "usergenes", handleGeneSymbolSearch( symbolLike, tier, taxon ) );

        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = {"symbolLike", "taxonId", "tier"})
    public ModelAndView searchUsersByGeneSymbolView( @RequestParam String symbolLike, @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        Taxon taxon = taxonService.findById( taxonId );
        ModelAndView modelAndView = new ModelAndView();

        modelAndView.addObject( "usergenes", handleGeneSymbolSearch( symbolLike, tier, taxon ) );
        modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = {"symbol", "taxonId", "tier"})
    public ModelAndView searchUsersByGene( @RequestParam String symbol, @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "manager/search" );
        if (gene == null) {
//            modelAndView.addObject( "users", new ArrayList<>() );
            modelAndView.addObject( "errorMessage", "Unknown Gene: " + symbol );
        } else {
            modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier ) );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = {"symbol", "taxonId", "tier"})
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        ModelAndView modelAndView = new ModelAndView();

        if (gene == null) {
//            modelAndView.addObject( "users", new ArrayList<>() );
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", "Unknown Gene: " + symbol );
        } else {
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
            modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier ) );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/manager/view/{userId}", method = RequestMethod.GET)
    public ModelAndView viewUser( @PathVariable Integer userId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        User viewUser = userService.findUserById( userId );

        if ( viewUser == null) {
            modelAndView.setViewName( "error/404" );
        } else {
            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", true );
            modelAndView.setViewName( "manager/view" );
        }
        return modelAndView;
    }

    private Collection<UserGene> handleGeneSymbolSearch( String symbolLike, TierType tier, Taxon taxon) {
        if ( tier.equals( TierType.ANY ) ) {
            return userGeneService.findByLikeSymbol( symbolLike, taxon );
        } else if ( tier.equals( TierType.MANUAL ) ) {
            return userGeneService.findByLikeSymbol( symbolLike, taxon, TierType.MANUAL_TIERS );
        } else {
            return userGeneService.findByLikeSymbol( symbolLike, taxon, tier );
        }
    }

    private Collection<UserGene> handleGeneSearch( Gene gene, TierType tier) {
        // TODO: Also search by exact symbol?
        if ( tier.equals( TierType.ANY ) ) {
            return userGeneService.findByGene( gene.getGeneId() );
        } else if ( tier.equals( TierType.MANUAL ) ) {
            return userGeneService.findByGene(  gene.getGeneId(), TierType.MANUAL_TIERS );
        } else {
            return userGeneService.findByGene(  gene.getGeneId(), tier );
        }
    }
}
