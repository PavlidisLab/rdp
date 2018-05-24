package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.UserService;

import javax.validation.Valid;

/**
 * Created by mjacobson on 16/01/18.
 */
@Controller
public class LoginController {

    private static Log log = LogFactory.getLog( LoginController.class );

    @Autowired
    private UserService userService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @RequestMapping(value = {"/login"}, method = RequestMethod.GET)
    public ModelAndView login() {
        ModelAndView modelAndView = new ModelAndView();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth instanceof AnonymousAuthenticationToken)) {
            // User is logged in
            return new ModelAndView("forward:/");
        }

        modelAndView.setViewName( "login" );
        return modelAndView;
    }


    @RequestMapping(value = "/registration", method = RequestMethod.GET)
    public ModelAndView registration() {
        ModelAndView modelAndView = new ModelAndView();
        User user = new User();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "registration" );
        return modelAndView;
    }

    @RequestMapping(value = "/registration", method = RequestMethod.POST)
    public ModelAndView createNewUser( @Valid User user, BindingResult bindingResult ) {
        ModelAndView modelAndView = new ModelAndView();
        User userExists = userService.findUserByEmail( user.getEmail() );
        if ( userExists != null ) {
            bindingResult
                    .rejectValue( "email", "error.user",
                            "There is already a user registered with the email provided" );
        }
        if ( bindingResult.hasErrors() ) {
            modelAndView.setViewName( "registration" );
        } else {
            userService.create( user );
            try {
                eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user ) );
                modelAndView.addObject( "message", "User has been registered successfully, we have sent you an email in order to confirm your account." );
            } catch (Exception me) {
                log.error(me);
                modelAndView.addObject( "message", "There was a problem sending the confirmation email. Please try again later." );
            }


            modelAndView.addObject( "user", new User() );
            modelAndView.setViewName( "registration" );

        }
        return modelAndView;
    }

    @RequestMapping(value = {"/resendConfirmation"}, method = RequestMethod.GET)
    public ModelAndView resendConfirmation() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "resendConfirmation" );
        return modelAndView;
    }

    @RequestMapping(value = "/resendConfirmation", method = RequestMethod.POST)
    public ModelAndView resendConfirmation( @RequestParam("email") String email ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "resendConfirmation" );
        User user = userService.findUserByEmail( email );

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
            } catch (Exception me) {
                log.error( me );
                modelAndView.addObject( "message", "There was a problem sending the confirmation email. Please try again later." );
            }
        }

        return modelAndView;
    }

    @RequestMapping(value = "/registrationConfirm", method = RequestMethod.GET)
    public ModelAndView confirmRegistration( WebRequest request, Model model, @RequestParam("token") String token) {
        User user = userService.confirmVerificationToken( token );

        UserPrinciple principle = new UserPrinciple(user);
        Authentication auth = new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() );
        SecurityContextHolder.getContext().setAuthentication( auth );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "redirect:user/home" );
        return modelAndView;

    }


}
