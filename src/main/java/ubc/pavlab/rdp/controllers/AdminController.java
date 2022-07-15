package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.model.AccessToken;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.validation.Valid;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
@CommonsLog
@Secured("ROLE_ADMIN")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private SiteSettings siteSettings;

    /**
     * List all users
     */
    @GetMapping(value = "/admin/users")
    public Object getAllUsers() {
        Collection<User> users = userService.findAll().stream()
                .sorted( Comparator.comparing( u -> u.getProfile().getFullName() ) )
                .collect( Collectors.toList() );
        ModelAndView view = new ModelAndView( "admin/users" );
        view.addObject( "users", users );
        return view;
    }

    @GetMapping(value = "/admin/create-service-account")
    public Object viewCreateServiceAccount( User user ) {
        return "admin/create-service-account";
    }

    @PostMapping(value = "/admin/create-service-account")
    public Object createServiceAccount( @Validated(User.ValidationServiceAccount.class) User user, BindingResult bindingResult ) {
        String serviceEmail = user.getEmail() + '@' + siteSettings.getHostUri().getHost();

        if ( userService.findUserByEmailNoAuth( serviceEmail ) != null ) {
            bindingResult.rejectValue( "email", "error.user", "There is already a user registered this email." );
        }

        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/create-service-account", HttpStatus.BAD_REQUEST );
        }

        user.setEmail( serviceEmail );
        user.setEnabled( true );

        Profile profile = user.getProfile();
        profile.setPrivacyLevel( PrivacyLevelType.PRIVATE );
        profile.setShared( false );
        profile.setHideGenelist( false );
        profile.setContactEmailVerified( false );
        user.setProfile( profile );

        user = userService.createServiceAccount( user );

        return "redirect:/admin/users/" + user.getId();
    }

    /**
     * Retrieve a user's details.
     */
    @GetMapping(value = "/admin/users/{user}")
    public Object getUser( @PathVariable User user, ConfirmEmailForm confirmEmailForm ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        return "admin/user";
    }

    @PostMapping(value = "/admin/users/{user}/create-access-token")
    public Object createAccessTokenForUser( @PathVariable User user, RedirectAttributes redirectAttributes ) {
        AccessToken accessToken = userService.createAccessTokenForUser( user );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Successfully created an access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/{user}";
    }

    @PostMapping(value = "/admin/users/{user}/revoke-access-token/{accessToken}")
    public Object revokeAccessTn( @PathVariable User user, @PathVariable AccessToken accessToken, RedirectAttributes redirectAttributes ) {
        if ( !accessToken.getUser().equals( user ) ) {
            return ResponseEntity.notFound().build();
        }
        userService.revokeAccessToken( accessToken );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Revoked access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/{user}";
    }

    /**
     * Delete a given user.
     */
    @DeleteMapping(value = "/admin/users/{user}")
    public Object deleteUser( @PathVariable User user,
                              @Valid ConfirmEmailForm confirmEmailForm,
                              BindingResult bindingResult ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }

        if ( !user.getEmail().equals( confirmEmailForm.getEmail() ) ) {
            bindingResult.rejectValue( "email", "error.user.email.doesNotMatchConfirmation", "User email does not match confirmation." );
        }

        if ( bindingResult.hasErrors() ) {
            ModelAndView view = new ModelAndView( "admin/user" );
            view.setStatus( HttpStatus.BAD_REQUEST );
            return view;
        } else {
            userService.delete( user );
            return "redirect:/admin/users";
        }
    }

    @Data
    private static class ConfirmEmailForm {
        private String email;
    }
}
