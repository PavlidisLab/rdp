package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.UserService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class MainController {

    private static Log log = LogFactory.getLog( MainController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @RequestMapping(value = {"/", "/user/home"}, method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/home" );
        return modelAndView;
    }



    @RequestMapping(value = "/stats.html")
    public void handleStatsHTMLEndpoint( HttpServletResponse response ) throws IOException {
        response.sendRedirect( "/stats" );
    }

    @RequestMapping(value = {"/user/profile"}, method = RequestMethod.GET)
    public ModelAndView profile() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/profile" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/documentation"}, method = RequestMethod.GET)
    public ModelAndView documentation() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/documentation" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/faq"}, method = RequestMethod.GET)
    public ModelAndView faq() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/faq" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/support"}, method = RequestMethod.GET)
    public ModelAndView support() {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

    @RequestMapping(value = {"/user/support"}, method = RequestMethod.POST)
    public ModelAndView supportPost(HttpServletRequest request, @RequestParam("name") String name, @RequestParam("message") String message,
                                    @RequestParam(required = false) MultipartFile attachment ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        log.info( user.getProfile().getLastName() + ", " + user.getProfile().getName() + " (" + user.getEmail() +
                ") is attempting to contact support." );

        try {
            emailService.sendSupportMessage( message, name, user, request, attachment );
            modelAndView.addObject( "message", "Sent. We will get back to you shortly." );
            modelAndView.addObject( "success", true );
        } catch (MessagingException e) {
            log.error(e);
            modelAndView.addObject( "message", "There was a problem sending the support request. Please try again later." );
            modelAndView.addObject( "success", false );
        }



        modelAndView.setViewName( "user/support" );
        return modelAndView;
    }

}
