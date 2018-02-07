package ubc.pavlab.rdp.controllers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.exception.UserNotFoundException;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.UserService;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.util.UUID;

/**
 * Created by mjacobson on 23/01/18.
 */
@Controller
public class PasswordController {

    private static Log log = LogFactory.getLog( PasswordController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class PasswordReset {

        @Length(min = 6, message = "*Your password must have at least 6 characters")
        @NotEmpty(message = "*Please provide a new password")
        String newPassword;

        String passwordConfirm;


        private boolean isValid() {
            return this.newPassword.equals( this.passwordConfirm );
        }

    }

    @RequestMapping(value = {"/forgotPassword"}, method = RequestMethod.GET)
    public ModelAndView forgotPassword() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "forgotPassword" );
        return modelAndView;
    }

    @RequestMapping(value = {"/updatePassword"}, method = RequestMethod.GET)
    public ModelAndView changePassword( @RequestParam("id") int id, @RequestParam("token") String token) {
        ModelAndView modelAndView = new ModelAndView();

        try {
            userService.verifyPasswordResetToken( id, token );
        } catch (TokenException e) {
            modelAndView.addObject("error", e.getMessage() );
        }


        modelAndView.addObject("userId", id );
        modelAndView.addObject("token", token );

        modelAndView.addObject("passwordReset", new PasswordReset() );
        modelAndView.setViewName( "updatePassword" );
        return modelAndView;
    }

    @RequestMapping(value = "/forgotPassword", method = RequestMethod.POST)
    public ModelAndView resetPassword( @RequestParam("email") String userEmail ) throws MessagingException {
        //TODO: require captcha?
        User user = userService.findUserByEmail( userEmail );
        if ( user == null ) {
            throw new UserNotFoundException();
        }
        String token = UUID.randomUUID().toString();
        userService.createPasswordResetTokenForUser( user, token );
        emailService.sendResetTokenMessage( token, user );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "message", "Password reset instructions have been sent." );
        modelAndView.setViewName( "forgotPassword" );

        return modelAndView;
    }

    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    public ModelAndView showChangePasswordPage( @RequestParam("id") int id, @RequestParam("token") String token,
                                                @Valid PasswordReset passwordReset, BindingResult bindingResult ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !passwordReset.isValid() ) {
            bindingResult.rejectValue( "passwordConfirm", "error.passwordReset", "Passwords should match" );
        }

        if (bindingResult.hasErrors()) {
            modelAndView.setViewName( "updatePassword" );
            modelAndView.addObject("userId", id );
            modelAndView.addObject("token", token );
        } else {
            try {
                userService.changePasswordByResetToken( id, token, passwordReset.getNewPassword() );

                User user = userService.findUserByIdNoAuth( id );
                UserPrinciple principle = new UserPrinciple(user);
                Authentication auth = new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() );
                SecurityContextHolder.getContext().setAuthentication( auth );

                modelAndView.setViewName( "redirect:user/home" );
                modelAndView.addObject( "user", user );
            } catch (TokenException e) {
                modelAndView.addObject( "message", e.getMessage() );
                modelAndView.setViewName( "updatePassword" );
            }
        }

        // Log in

        return modelAndView;
    }

}
