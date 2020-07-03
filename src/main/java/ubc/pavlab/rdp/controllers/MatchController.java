package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;

import java.util.UUID;

@Controller
@CommonsLog
public class MatchController {

    @Autowired
    UserService userService;

    @Autowired
    UserGeneService userGeneService;

    @Autowired
    PrivacyService privacyService;

    /**
     * Request a match with a user.
     *
     * @param hiddenUserId
     * @return
     */
    @RequestMapping(value = "/match/user/{hiddenUserId}")
    public ModelAndView requestMatch( @PathVariable UUID hiddenUserId ) {
        // if the instance does not allow search, there's no way match can be requested
        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }
        User user = userService.findUserByHiddenId( hiddenUserId );
        ModelAndView view = new ModelAndView();
        if ( user == null ) {
            view.setViewName( "error/404" );
            view.setStatus( HttpStatus.NOT_FOUND );
            log.debug( "user was not found" );
        } else {
            view.setViewName( "match" );
            view.addObject( "hiddenUser", user );
            // is the null necessary here?
            view.addObject( "hiddenUserGene", null );
        }
        return view;
    }

    /**
     * Request a match with a user mediated by a gene.
     *
     * @param hiddenUserId
     * @param hiddenGeneId
     * @return
     */
    @RequestMapping(value = "/match/user/{hiddenUserId}/gene/{hiddenGeneId}", method = RequestMethod.GET)
    public ModelAndView requestGeneMatch( @PathVariable UUID hiddenUserId, @PathVariable UUID hiddenGeneId ) {
        // if the instance does not allow search, there's no way match can be requested
        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }
        User user = userService.findUserByHiddenId( hiddenUserId );
        UserGene userGene = userGeneService.findUserGeneByHiddenId( hiddenGeneId );
        ModelAndView view = new ModelAndView();
        if ( user == null ) {
            view.setViewName( "error/404" );
            view.setStatus( HttpStatus.NOT_FOUND );
        } else {
            view.setViewName( "match" );
            view.addObject( "hiddenUser", user );
            view.addObject( "hiddenUserGene", userGene );
        }
        return view;
    }
}
