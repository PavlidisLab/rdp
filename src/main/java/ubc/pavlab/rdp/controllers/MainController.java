package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

@Controller
public class MainController {

    private static Log log = LogFactory.getLog( MainController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GOService goService;

    @Autowired
    private EmailService emailService;

    @RequestMapping(value = {"/", "/user/home"}, method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/home" );
        return modelAndView;
    }



    @RequestMapping(value = "/stats.html")
    public void handleStatsHTMLEndpoint( HttpServletResponse response ) throws IOException {
        response.sendRedirect( "/stats" );
    }

    @RequestMapping(value = {"/user/model/{taxonId}"}, method = RequestMethod.GET)
    public ModelAndView model( @PathVariable Integer taxonId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        modelAndView.addObject( "user", user );
        modelAndView.addObject( "taxon", taxon );
        modelAndView.setViewName( "user/model" );
        return modelAndView;
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/recommend/view", method = RequestMethod.GET)
    public ModelAndView getRecommendedTermsForTaxon( @PathVariable Integer taxonId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        modelAndView.addObject( "userTerms",  userService.recommendTerms(user, taxon) );
        modelAndView.addObject( "viewOnly", true);
        modelAndView.setViewName( "fragments/term-table :: term-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/{goId}/gene/view", method = RequestMethod.GET)
    public ModelAndView getTermsGenesForTaxon( @PathVariable Integer taxonId, @PathVariable String goId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        Collection<Gene> genes = goService.getGenes( goService.getTerm( goId ) );

        modelAndView.addObject( "genes", user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS )
                .stream().filter( genes::contains ).collect( Collectors.toSet() ));
        modelAndView.addObject( "viewOnly", true);
        modelAndView.setViewName( "fragments/gene-table :: gene-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/user/{userId}/profile", method = RequestMethod.GET)
    public ModelAndView viewUserProfile( @PathVariable Integer userId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findUserById( userId );

        modelAndView.addObject( "user", user );
        modelAndView.addObject( "viewOnly", true);
        modelAndView.setViewName( "user/profile" );
        return modelAndView;
    }

    @RequestMapping(value = "/user/{userId}/model/{taxonId}", method = RequestMethod.GET)
    public ModelAndView viewUserModel( @PathVariable Integer userId, @PathVariable Integer taxonId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findUserById( userId );

        Taxon taxon = taxonService.findById( taxonId );

        modelAndView.addObject( "user", user );
        modelAndView.addObject( "taxon", taxon );
        modelAndView.addObject( "viewOnly", true);
        modelAndView.setViewName( "user/model" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/profile"}, method = RequestMethod.GET)
    public ModelAndView profile() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/profile" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/documentation"}, method = RequestMethod.GET)
    public ModelAndView documentation() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/documentation" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/faq"}, method = RequestMethod.GET)
    public ModelAndView faq() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/faq" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/support"}, method = RequestMethod.GET)
    public ModelAndView support() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

    @RequestMapping(value = {"/manager/search"}, method = RequestMethod.GET)
    public ModelAndView managerSearch() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = {"/maintenance"}, method = RequestMethod.GET)
    public ModelAndView maintenance() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "error/maintenance" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/support"}, method = RequestMethod.POST)
    public ModelAndView supportPost(HttpServletRequest request, @RequestParam("name") String name, @RequestParam("message") String message,
                                    @RequestParam(required = false) MultipartFile attachment ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        log.info( user.getProfile().getLastName() + ", " + user.getProfile().getName() + " (" + user.getEmail() +
                ") is attempting to contact support." );

        try {
            emailService.sendSupportMessage( message, name, user, request, attachment );
            modelAndView.addObject( "message", "Sent. We will get back to you shortly." );
            modelAndView.addObject( "success", true );
        } catch (MessagingException e) {
            log.error(e);
            modelAndView.addObject( "message", "There was a problem sending the support request. Please try again later." );
            modelAndView.addObject( "success", false );
        }



        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

}
