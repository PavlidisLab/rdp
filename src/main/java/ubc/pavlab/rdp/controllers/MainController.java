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
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.UserNotFoundException;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.UserService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Controller
public class MainController {

    private static Log log = LogFactory.getLog( MainController.class );

    @Autowired
    private Environment env;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;


    @Setter
    @NoArgsConstructor
    static class ContactSupport {

        @NotEmpty(message = "*Please provide a name")
        String name;

        @NotEmpty(message = "*Please provide a message")
        String message;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class PasswordReset {

        @Length(min = 6, message = "*Your password must have at least 6 characters")
        @NotEmpty(message = "*Please provide a new password")
        String newPassword;

        @Length(min = 6, message = "*Your password must have at least 6 characters")
        @NotEmpty(message = "*Please confirm your password")
        String passwordConfirm;

        @AssertTrue(message = "Passwords should match")
        private boolean isValid() {
            return this.newPassword.equals( this.passwordConfirm );
        }
    }

    @RequestMapping(value = {"/"}, method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "ga_tracker", env.getProperty( "ga.tracker" ) );
        modelAndView.addObject( "ga_domain", env.getProperty( "ga.domain" ) );
        modelAndView.setViewName( "admin/home" );
        return modelAndView;
    }

    @RequestMapping(value = {"/forgotPassword"}, method = RequestMethod.GET)
    public ModelAndView forgotPassword() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "forgotPassword" );
        return modelAndView;
    }

    @RequestMapping(value = {"/updatePassword"}, method = RequestMethod.GET)
    public ModelAndView changePassword(@RequestParam("id") int id, @RequestParam("token") String token) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("userId", id );
        modelAndView.addObject("token", token );
        modelAndView.addObject("reset", new PasswordReset() );
        modelAndView.setViewName( "updatePassword" );
        return modelAndView;
    }

    @RequestMapping(value = "/stats.html")
    public void handleStatsHTMLEndpoint( HttpServletResponse response ) throws IOException {
        response.sendRedirect( "/stats" );
    }

    @RequestMapping(value = "/contactSupport.html", method = RequestMethod.POST)
    public String contactSupport( HttpServletRequest request, @RequestBody ContactSupport contact,
                                  @RequestParam(required = false) CommonsMultipartFile attachment ) throws MessagingException {

        User user = userService.findCurrentUser();

        log.info( user.getProfile().getLastName() + ", " + user.getProfile().getName() + "(" + user.getEmail() +
                ") is attempting to contact support." );

        emailService.sendSupportMessage( contact.message, contact.name, user, request, attachment );

        return "Done.";
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
                                          @Valid PasswordReset reset ) {


        userService.changePasswordByResetToken( id, token, reset.getNewPassword() );

        // Log in
        User user = userService.findUserById( id );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(
                new SimpleGrantedAuthority( "ROLE_USER" ) ) );

        SecurityContextHolder.getContext().setAuthentication(auth);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "admin/home" );
        modelAndView.addObject("user", user);
        modelAndView.addObject("message","Password Updated");
        return modelAndView;
    }


}
