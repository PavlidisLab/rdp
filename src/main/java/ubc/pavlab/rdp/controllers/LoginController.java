package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserPrinciple;
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
        ModelAndView modelAndView = new ModelAndView();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if ( !( auth instanceof AnonymousAuthenticationToken ) ) {
            // User is logged in
            return new ModelAndView( "forward:/" );
        }

        modelAndView.setViewName( "login" );
        return modelAndView;
    }


    @GetMapping("/registration")
    public ModelAndView registration() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", new User() );
        modelAndView.setViewName( "registration" );
        return modelAndView;
    }

    @PostMapping("/registration")
    public String createNewUser( @Valid User user,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes ) {
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

        if ( !bindingResult.hasErrors() ) {
            user = userService.create( user );
            try {
                eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user ) );
                redirectAttributes.addAttribute( "message", "Your user account was registered successfully. Please check your email for completing the completing the registration process." );
            } catch ( Exception me ) {
                log.error( me );
                redirectAttributes.addAttribute( "message", "Your user account was registered successfully, but we couldn't send you a confirmation email." );
            } finally {
                return "redirect:/login";
            }
        }

        return "registration";
    }

    @RequestMapping(value = { "/resendConfirmation" }, method = RequestMethod.GET)
    public ModelAndView resendConfirmation() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "resendConfirmation" );
        return modelAndView;
    }

    @RequestMapping(value = "/resendConfirmation", method = RequestMethod.POST)
    public ModelAndView resendConfirmation( @RequestParam("email") String email ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "resendConfirmation" );
        User user = userService.findUserByEmailNoAuth( email );

        if ( user == null ) {
//            modelAndView.addObject( "message", "User does not exist." );
            modelAndView.addObject( "message", "Confirmation email sent." );
            return modelAndView;
        } else if ( user.isEnabled() ) {
//            modelAndView.addObject( "message", "Account already enabled." );
            modelAndView.addObject( "message", "Confirmation email sent." );
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

    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public ModelAndView confirmRegistration( WebRequest request, Model model, @RequestParam("token") String token ) {
        User user = userService.confirmVerificationToken( token );

        UserPrinciple principle = new UserPrinciple( user );
        Authentication auth = new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() );
        SecurityContextHolder.getContext().setAuthentication( auth );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "redirect:user/home" );
        return modelAndView;

    }


}
