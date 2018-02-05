package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;

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
    public ModelAndView searchUsersByGeneSymbol( @RequestParam String symbolLike,  @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        if (tier.equals( TierType.UNKNOWN )) {
            modelAndView.addObject( "users", userService.findByLikeSymbol( symbolLike, taxon ) );
        } else {
            modelAndView.addObject( "users", userService.findByLikeSymbol( symbolLike, taxon, tier ) );
        }

        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = {"symbolLike", "taxonId", "tier"})
    public ModelAndView searchUsersByGeneSymbolView(@RequestParam String symbolLike,  @RequestParam Integer taxonId, @RequestParam TierType tier ) {
        Taxon taxon = taxonService.findById( taxonId );
        ModelAndView modelAndView = new ModelAndView();
        if (tier.equals( TierType.UNKNOWN )) {
            modelAndView.addObject( "users", userService.findByLikeSymbol( symbolLike, taxon ) );
        } else {
            modelAndView.addObject( "users", userService.findByLikeSymbol( symbolLike, taxon, tier ) );
        }
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }
}
