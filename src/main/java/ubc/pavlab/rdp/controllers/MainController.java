package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.util.Messages;

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Controller
@CommonsLog
public class MainController {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    @PreAuthorize("hasPermission(null, 'search')")
    @GetMapping(value = { "/" })
    public String index() {
        return userService.findCurrentUser() == null ? "redirect:/search" : "redirect:/user/home";
    }

    @GetMapping(value = { "/maintenance" })
    public String maintenance() {
        return "error/maintenance";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/gettimeout", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseBody
    public ResponseEntity<?> getTimeout( HttpSession httpSession ) {
        // Only set timeout cookie if the user is authenticated.
        Instant currTime = Instant.now();
        Duration timeoutInSeconds = Duration.ofSeconds( httpSession.getMaxInactiveInterval() ).minusSeconds( 60 ); // Subtracting by 60s to give an extra minute client-side.
        Instant expiryTime = currTime.plus( timeoutInSeconds );

        // Get cookie for server current time.
        ResponseCookie serverTimeCookie = ResponseCookie.from( "serverTime", Long.toString( currTime.toEpochMilli() ) )
                .path( "/" )
                .build();

        // Get cookie for expiration time (consistent with serverTime cookie).
        ResponseCookie sessionExpiryCookie = ResponseCookie.from( "sessionExpiry", Long.toString( expiryTime.toEpochMilli() ) )
                .path( "/" )
                .build();

        return ResponseEntity.noContent()
                .header( HttpHeaders.SET_COOKIE, serverTimeCookie.toString() )
                .header( HttpHeaders.SET_COOKIE, sessionExpiryCookie.toString() )
                .build();
    }

    @GetMapping(value = "/terms-of-service")
    public ModelAndView termsOfService( Locale locale ) {
        try {
            return new ModelAndView( "terms-of-service" )
                    .addObject( "termsOfService", messageSource.getMessage( "rdp.terms-of-service", new Object[]{ Messages.SHORTNAME, Messages.FULLNAME }, locale ) );
        } catch ( NoSuchMessageException e ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", "No terms of service document is setup for this registry." );
        }
    }

    @GetMapping(value = "/privacy-policy")
    public ModelAndView privacyPolicy( Locale locale ) {
        try {
            return new ModelAndView( "privacy-policy" )
                    .addObject( "privacyPolicy", messageSource.getMessage( "rdp.privacy-policy", new Object[]{ Messages.SHORTNAME, Messages.FULLNAME }, locale ) );
        } catch ( NoSuchMessageException e ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", "No privacy policy is setup for this registry." );
        }
    }
}
