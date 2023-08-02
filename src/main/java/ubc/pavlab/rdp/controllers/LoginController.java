package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.exception.TokenNotFoundException;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.ExpiredTokenException;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.util.Locale;

/**
 * Created by mjacobson on 16/01/18.
 */
@Controller
@CommonsLog
public class LoginController {

    @Autowired
    private UserService userService;

    @Autowired
    private PrivacyService privacyService;

    @Autowired
    private ApplicationSettings applicationSettings;

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
    public ModelAndView createNewUser( @Validated(User.ValidationUserAccount.class) User user,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes,
                                       Locale locale ) {
        ModelAndView modelAndView = new ModelAndView( "registration" );
        User existingUser = userService.findUserByEmailNoAuth( user.getEmail() );

        user.setEnabled( false );

        // initialize a basic user profile
        Profile userProfile = user.getProfile();
        userProfile.setPrivacyLevel( privacyService.getDefaultPrivacyLevel() );
        userProfile.setShared( applicationSettings.getPrivacy().isDefaultSharing() );
        userProfile.setHideGenelist( false );
        userProfile.setContactEmailVerified( false );

        if ( existingUser != null ) {
            if ( existingUser.isEnabled() ) {
                bindingResult.rejectValue( "email", "error.user", "There is already a user registered this email." );
            } else {
                // maybe the user is attempting to re-register, unaware that the confirmation hasn't been processed
                userService.createVerificationTokenForUser( existingUser, locale );
                bindingResult.rejectValue( "email", "error.user", "You have already registered an account with this email. We just sent you a new confirmation email." );
            }
            log.warn( "Trying to register an already registered email: " + user.getEmail() + "." );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
        } else {
            user = userService.create( user );
            userService.createVerificationTokenForUser( user, locale );
            redirectAttributes.addFlashAttribute( "message", "Your user account was registered successfully. Please check your email for completing the completing the registration process." );
            modelAndView.setViewName( "redirect:/login" );
        }

        return modelAndView;
    }

    @GetMapping(value = "/resendConfirmation")
    public ModelAndView resendConfirmation() {
        return new ModelAndView( "resendConfirmation" );
    }

    @PostMapping(value = "/resendConfirmation")
    public ModelAndView resendConfirmation( @RequestParam("email") String email, Locale locale ) {
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
            userService.createVerificationTokenForUser( user, locale );
            modelAndView.addObject( "message", "Confirmation email sent." );
        }

        return modelAndView;
    }

    @GetMapping(value = "/registrationConfirm")
    public ModelAndView confirmRegistration( @RequestParam("token") String token,
                                             RedirectAttributes redirectAttributes ) {
        try {
            userService.confirmVerificationToken( token );
            redirectAttributes.addFlashAttribute( "message", "Your account has been enabled successfully, and you can now proceed to login." );
            return new ModelAndView( "redirect:/login" );
        } catch ( TokenException e ) {
            log.warn( String.format( "Could not confirm registration token: %s.", e.getMessage() ) );
            if ( e instanceof TokenNotFoundException ) {
                // this is our best guess
                redirectAttributes.addFlashAttribute( "message", "The registration link was already used. You may now proceed to login." );
                return new ModelAndView( "redirect:/login" );
            } else if ( e instanceof ExpiredTokenException ) {
                redirectAttributes.addFlashAttribute( "message", "The registration link is expired." );
                return new ModelAndView( "redirect:/resendConfirmation" );
            } else {
                return new ModelAndView( "registrationConfirm", HttpStatus.BAD_REQUEST )
                        .addObject( "message", "The registration link is invalid." )
                        .addObject( "error", Boolean.TRUE );
            }
        }
    }
}
