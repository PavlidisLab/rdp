package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.PasswordReset;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.UserService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Locale;

/**
 * Created by mjacobson on 23/01/18.
 */
@Controller
@CommonsLog
public class PasswordController {

    @Autowired
    private UserService userService;

    @GetMapping(value = "/forgotPassword")
    public ModelAndView forgotPassword() {
        return new ModelAndView( "forgotPassword" );
    }

    @PostMapping(value = "/forgotPassword")
    public ModelAndView resetPassword( @RequestParam("email") String userEmail, Locale locale ) {
        //TODO: require captcha?
        User user = userService.findUserByEmailNoAuth( userEmail );

        ModelAndView modelAndView = new ModelAndView( "forgotPassword" );

        if ( user == null ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.addObject( "message", "User not found." );
            modelAndView.addObject( "error", Boolean.TRUE );
            return modelAndView;
        }

        userService.createPasswordResetTokenForUser( user, locale );

        modelAndView.addObject( "message", "Password reset instructions have been sent." );
        modelAndView.addObject( "error", Boolean.FALSE );

        return modelAndView;
    }

    @GetMapping(value = "/updatePassword")
    public ModelAndView changePassword( @RequestParam("id") int id, @RequestParam("token") String token, HttpServletRequest request ) {
        ModelAndView modelAndView = new ModelAndView( "updatePassword" );

        try {
            userService.verifyPasswordResetToken( id, token, request );
        } catch ( TokenException e ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "error", e.getMessage() );
        }

        modelAndView.addObject( "userId", id );
        modelAndView.addObject( "token", token );

        modelAndView.addObject( "passwordReset", new PasswordReset() );
        return modelAndView;
    }

    @PostMapping(value = "/updatePassword")
    public ModelAndView showChangePasswordPage( @RequestParam("id") int id, @RequestParam("token") String token,
                                                @Valid PasswordReset passwordReset, BindingResult bindingResult,
                                                HttpServletRequest request ) {
        ModelAndView modelAndView = new ModelAndView( "updatePassword" );

        if ( !passwordReset.isValid() ) {
            bindingResult.rejectValue( "passwordConfirm", "error.passwordReset", "Password confirmation does not match new password." );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "userId", id );
            modelAndView.addObject( "token", token );
            return modelAndView;
        }

        try {
            userService.changePasswordByResetToken( id, token, passwordReset, request );
        } catch ( TokenException e ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", e.getMessage() );
            return modelAndView;
        }

        // Log in
        User user = userService.findUserByIdNoAuth( id );
        UserPrinciple principle = new UserPrinciple( user );
        Authentication auth = new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() );
        SecurityContextHolder.getContext().setAuthentication( auth );
        modelAndView.setViewName( "redirect:/user/home" );
        modelAndView.addObject( "user", user );
        return modelAndView;
    }
}
