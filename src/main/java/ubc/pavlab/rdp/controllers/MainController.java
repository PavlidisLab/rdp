package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ubc.pavlab.rdp.services.UserService;

import javax.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;

@Controller
@CommonsLog
public class MainController {

    @Autowired
    private UserService userService;

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
    public String termsOfService() {
        return "terms-of-service";
    }

    @GetMapping(value = "/privacy-policy")
    public String privacyPolicy() {
        return "privacy-policy";
    }
}
