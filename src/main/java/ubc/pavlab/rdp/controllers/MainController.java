package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.mail.MessagingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@CommonsLog
public class MainController {

    private static Role adminRole;

    @Autowired
    private UserService userService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GOService goService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private RoleRepository roleRepository;

    @RequestMapping(value = { "/" }, method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        if(userService.findCurrentUser() == null){
            modelAndView.setViewName( "redirect:" + (applicationSettings.getPrivacy().isPublicSearch() ? "/search" :  "/login") );
        }else{
            modelAndView.setViewName( "redirect:user/home" );
        }
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/home" }, method = RequestMethod.GET)
    public ModelAndView userHome() {
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

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/model/{taxonId}" }, method = RequestMethod.GET)
    public ModelAndView model( @PathVariable Integer taxonId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        modelAndView.addObject( "viewOnly", null );
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "taxon", taxon );
        modelAndView.setViewName( "user/model" );
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = "/user/taxon/{taxonId}/term/{goId}/gene/view", method = RequestMethod.GET)
    public ModelAndView getTermsGenesForTaxon( @PathVariable Integer taxonId, @PathVariable String goId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        GeneOntologyTerm term = goService.getTerm( goId );

        if ( term != null ) {
            Set<Integer> geneIds = goService.getGenes( term ).stream().map( GeneInfo::getGeneId ).collect( Collectors.toSet() );
            modelAndView.addObject( "genes",
                    user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ).stream()
                            .filter( ug -> geneIds.contains( ug.getGeneId() ) )
                            .collect( Collectors.toSet() ) );
        } else {
            modelAndView.addObject( "genes", Collections.EMPTY_SET );
        }
        modelAndView.addObject( "viewOnly", true );
        modelAndView.setViewName( "fragments/gene-table :: gene-table" );
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/profile" }, method = RequestMethod.GET)
    public ModelAndView profile() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "viewOnly", null );
        modelAndView.setViewName( "user/profile" );
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/documentation" }, method = RequestMethod.GET)
    public ModelAndView documentation() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/documentation" );
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/faq" }, method = RequestMethod.GET)
    public ModelAndView faq() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/faq" );
        return modelAndView;
    }

    @Secured({ "ROLE_USER", "ROLE_ADMIN" })
    @RequestMapping(value = { "/user/support" }, method = RequestMethod.GET)
    public ModelAndView support() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

    @RequestMapping(value = { "/search" }, method = RequestMethod.GET)
    public ModelAndView search() {
        User user = userService.findCurrentUser();
        if ( searchAuthorized(user) ) {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.addObject( "chars", userService.getLastNamesFirstChar() );
            modelAndView.addObject( "user", user );
            modelAndView.setViewName( "search" );
            return modelAndView;
        } else {
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName( "login" );
            return modelAndView;
        }

    }

    @RequestMapping(value = { "/maintenance" }, method = RequestMethod.GET)
    public ModelAndView maintenance() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "error/maintenance" );
        return modelAndView;
    }

    @RequestMapping(value = { "/user/support" }, method = RequestMethod.POST)
    public ModelAndView supportPost( HttpServletRequest request,
                                     @RequestParam String name,
                                     @RequestParam String message,
                                     @RequestParam(required = false) MultipartFile attachment ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        log.info( user.getProfile().getLastName() + ", " + user.getProfile().getName() + " (" + user.getEmail()
                + ") is attempting to contact support." );

        try {
            emailService.sendSupportMessage( message, name, user, request, attachment );
            modelAndView.addObject( "message", "Sent. We will get back to you shortly." );
            modelAndView.addObject( "success", true );
        } catch ( MessagingException e ) {
            log.error( e );
            modelAndView
                    .addObject( "message", "There was a problem sending the support request. Please try again later." );
            modelAndView.addObject( "success", false );
        }

        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

    
    @RequestMapping(value="/gettimeout", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ModelAndView getTimeout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
    // public ResponseEntity<Object> getTimeout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
	// Set cookie
	addTimeoutCookies(servletRequest, servletResponse);

	ModelAndView modelAndView = new ModelAndView();
	modelAndView.addObject( "message", "Session timeout refreshed." );

	//return new ResponseEntity<Object>(HttpStatus.OK);
	return modelAndView;
    }

    private void addTimeoutCookies(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        User user = userService.findCurrentUser();
	if ( user != null ) {
	    // Only set timeout cookie if the user is authenticated.	    
	    long currTime = System.currentTimeMillis();	    
	    int TIMEOUT_IN_SECONDS = servletRequest.getSession().getMaxInactiveInterval() - 60; // Subtracting by 60s to give an extra minute client-side.
	    long expiryTime = currTime + TIMEOUT_IN_SECONDS * 1000;

	    // Get cookie for server current time.
	    Cookie serverTimeCookie = new Cookie("serverTime", "" + currTime);
	    serverTimeCookie.setPath("/");
	    servletResponse.addCookie(serverTimeCookie);

	    // Get cookie for expiration time (consistent with serverTime cookie).
	    Cookie expiryCookie = new Cookie("sessionExpiry", "" + expiryTime);
	    expiryCookie.setPath("/");
	    servletResponse.addCookie(expiryCookie);
	}
    }

    private boolean searchAuthorized(User user){
        if(adminRole == null) {
            adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        }
        return applicationSettings.getPrivacy().isPublicSearch() // Search is public
                || ( applicationSettings.getPrivacy().isRegisteredSearch() && user != null ) // Search is registered and there is user logged
                || ( user != null && adminRole != null && user.getRoles().contains( adminRole ) ); // User is admin
    }

    private Stats getStats() {
        return new Stats(
                userService.countResearchers(),
                userGeneService.countUsersWithGenes(),
                userGeneService.countAssociations(),
                userGeneService.countUniqueAssociations(),
                userGeneService.countUniqueAssociationsAllTiers(),
                userGeneService.countUniqueAssociationsToHumanAllTiers(),
                userGeneService.researcherCountByTaxon() );
    }

}
