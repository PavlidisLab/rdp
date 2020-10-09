package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.validation.Valid;

/**
 * Created by mjacobson on 16/01/18.
 */
@Controller
@CommonsLog
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    PrivacyService privacyService;

    @Autowired
    ApplicationSettings applicationSettings;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @GetMapping("/login")
    public ModelAndView login() {
        ModelAndView modelAndView = new ModelAndView( "login" );

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ( !( auth instanceof AnonymousAuthenticationToken ) ) {
            // User is logged in
            return new ModelAndView( "forward:/" );
        }

        return modelAndView;
    }

    @GetMapping("/registration")
    public ModelAndView registration() {
        ModelAndView modelAndView = new ModelAndView( "registration" );
        modelAndView.addObject( "user", new User() );
        return modelAndView;
    }

    @PostMapping("/registration")
    public ModelAndView createNewUser( @Valid User user,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes ) {
        ModelAndView modelAndView = new ModelAndView( "registration" );
        User userExists = userService.findUserByEmailNoAuth( user.getEmail() );

        // initialize a basic user profile
        Profile userProfile = user.getProfile();
        userProfile.setPrivacyLevel( privacyService.getDefaultPrivacyLevel() );
        userProfile.setShared( applicationSettings.getPrivacy().isDefaultSharing() );
        userProfile.setHideGenelist( false );

        if ( userExists != null ) {
            bindingResult.rejectValue( "email", "error.user", "There is already a user registered this email." );
            log.warn( "Trying to register an already registered email." );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
        } else {
            user = userService.create( user );
            try {
                eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user ) );
                redirectAttributes.addFlashAttribute( "message", "Your user account was registered successfully. Please check your email for completing the completing the registration process." );
            } catch ( Exception me ) {
                log.error( me );
                redirectAttributes.addFlashAttribute( "message", "Your user account was registered successfully, but we couldn't send you a confirmation email." );
            } finally {
                modelAndView.setViewName( "redirect:/login" );
            }
        }

        return modelAndView;
    }

    @GetMapping(value = "/resendConfirmation")
    public ModelAndView resendConfirmation() {
        ModelAndView modelAndView = new ModelAndView( "resendConfirmation" );
        return modelAndView;
    }

    @PostMapping(value = "/resendConfirmation")
    public ModelAndView resendConfirmation( @RequestParam("email") String email ) {
        ModelAndView modelAndView = new ModelAndView( "resendConfirmation" );
        User user = userService.findUserByEmailNoAuth( email );

        // don't leak the user existence
        if ( user == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message", "User does not exist." );
            return modelAndView;
        } else if ( user.isEnabled() ) {
            // user is already enabled...
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", "User is already enabled." );
            return modelAndView;
        } else {

            try {
                eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user ) );
                modelAndView.addObject( "message", "Confirmation email sent." );
            } catch ( Exception me ) {
                log.error( me );
                modelAndView.addObject( "message", "There was a problem sending the confirmation email. Please try again later." );
            }
        }

        return modelAndView;
    }

    @GetMapping(value = "/registrationConfirm")
    public ModelAndView confirmRegistration( @RequestParam("token") String token ) {
        ModelAndView modelAndView = new ModelAndView();

        try {
            userService.confirmVerificationToken( token );
            modelAndView.setViewName( "redirect:/login" );
        } catch ( TokenException e ) {
            log.error( e );
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
        }

        return modelAndView;
    }


}
