package ubc.pavlab.rdp.controllers;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.TokenException;
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
@CommonsLog
public class PasswordController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Data
    static class PasswordReset {

        @Length(min = 6, message = "New password must have at least 6 characters.")
        String newPassword;

        @NotEmpty(message = "Password confirmation cannot be empty.")
        String passwordConfirm;

        boolean isValid() {
            return this.newPassword.equals( this.passwordConfirm );
        }
    }

    @Data
    static class PasswordChange extends PasswordReset {

        @NotEmpty(message = "Current password cannot be empty.")
        String oldPassword;
    }

    @RequestMapping(value = { "/forgotPassword" }, method = RequestMethod.GET)
    public ModelAndView forgotPassword() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "forgotPassword" );
        return modelAndView;
    }

    @RequestMapping(value = { "/updatePassword" }, method = RequestMethod.GET)
    public ModelAndView changePassword( @RequestParam("id") int id, @RequestParam("token") String token ) {
        ModelAndView modelAndView = new ModelAndView();

        try {
            userService.verifyPasswordResetToken( id, token );
        } catch ( TokenException e ) {
            modelAndView.addObject( "error", e.getMessage() );
        }

        modelAndView.addObject( "userId", id );
        modelAndView.addObject( "token", token );

        modelAndView.addObject( "passwordReset", new PasswordReset() );
        modelAndView.setViewName( "updatePassword" );
        return modelAndView;
    }

    @RequestMapping(value = "/forgotPassword", method = RequestMethod.POST)
    public ModelAndView resetPassword( @RequestParam("email") String userEmail ) throws MessagingException {
        //TODO: require captcha?
        User user = userService.findUserByEmailNoAuth( userEmail );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "forgotPassword" );

        if ( user == null ) {
            modelAndView.addObject( "message", "User not found" );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }
        String token = UUID.randomUUID().toString();
        userService.createPasswordResetTokenForUser( user, token );
        emailService.sendResetTokenMessage( token, user );

        modelAndView.addObject( "message", "Password reset instructions have been sent." );
        modelAndView.addObject( "error", false );

        return modelAndView;
    }

    @RequestMapping(value = "/updatePassword", method = RequestMethod.POST)
    public ModelAndView showChangePasswordPage( @RequestParam("id") int id, @RequestParam("token") String token,
                                                @Valid PasswordReset passwordReset, BindingResult bindingResult ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !passwordReset.isValid() ) {
            bindingResult.rejectValue( "passwordConfirm", "error.passwordReset", "Password confirmation does not match new password." );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setViewName( "updatePassword" );
            modelAndView.addObject( "userId", id );
            modelAndView.addObject( "token", token );
        } else {
            try {
                userService.changePasswordByResetToken( id, token, passwordReset.getNewPassword() );

                User user = userService.findUserByIdNoAuth( id );
                UserPrinciple principle = new UserPrinciple( user );
                Authentication auth = new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() );
                SecurityContextHolder.getContext().setAuthentication( auth );

                modelAndView.setViewName( "redirect:user/home" );
                modelAndView.addObject( "user", user );
            } catch ( TokenException e ) {
                modelAndView.addObject( "message", e.getMessage() );
                modelAndView.setViewName( "updatePassword" );
            }
        }

        // Log in

        return modelAndView;
    }

    @GetMapping(value = { "/user/password" })
    public ModelAndView changePassword() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        modelAndView.addObject( "passwordChange", new PasswordChange() );
        modelAndView.setViewName( "user/password" );
        return modelAndView;
    }

    @PostMapping(value = "/user/password")
    public ModelAndView changePassword( @Valid PasswordChange passwordChange, BindingResult bindingResult ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "user/password" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        if ( !passwordChange.isValid() ) {
            bindingResult.rejectValue( "passwordConfirm", "error.passwordChange", "Password conformation does not match new password." );
        }

        if ( bindingResult.hasErrors() ) {
            // Short circuit before testing password.
            return modelAndView;
        }

        try {
            userService.changePassword( passwordChange.oldPassword, passwordChange.newPassword );
        } catch ( BadCredentialsException e ) {
            bindingResult.rejectValue( "oldPassword", "error.passwordChange", "Current password does not match." );
        }

        if ( !bindingResult.hasErrors() ) {
            modelAndView.addObject( "passwordChange", new PasswordChange() );
            modelAndView.addObject( "message", "Password Updated" );
        }

        return modelAndView;
    }

}
