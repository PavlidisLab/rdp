package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.services.UserService;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

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

    @RequestMapping(value = "/stats.html")
    public void handleStatsHTMLEndpoint( HttpServletResponse response ) throws IOException {
        response.sendRedirect( "/stats" );
    }

    @GetMapping(value = { "/maintenance" })
    public ModelAndView maintenance() {
        ModelAndView modelAndView = new ModelAndView( "error/maintenance" );
        return modelAndView;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(value = "/gettimeout", produces = MediaType.TEXT_PLAIN)
    @ResponseBody
    public String getTimeout( HttpServletRequest servletRequest, HttpServletResponse servletResponse ) {
        addTimeoutCookies( servletRequest, servletResponse );
        return "Session timeout refreshed.";
    }

    private void addTimeoutCookies( HttpServletRequest servletRequest, HttpServletResponse servletResponse ) {
        // Only set timeout cookie if the user is authenticated.
        long currTime = System.currentTimeMillis();
        int TIMEOUT_IN_SECONDS = servletRequest.getSession().getMaxInactiveInterval() - 60; // Subtracting by 60s to give an extra minute client-side.
        long expiryTime = currTime + TIMEOUT_IN_SECONDS * 1000;

        // Get cookie for server current time.
        Cookie serverTimeCookie = new Cookie( "serverTime", "" + currTime );
        serverTimeCookie.setPath( "/" );
        servletResponse.addCookie( serverTimeCookie );

        // Get cookie for expiration time (consistent with serverTime cookie).
        Cookie expiryCookie = new Cookie( "sessionExpiry", "" + expiryTime );
        expiryCookie.setPath( "/" );
        servletResponse.addCookie( expiryCookie );
    }
}
