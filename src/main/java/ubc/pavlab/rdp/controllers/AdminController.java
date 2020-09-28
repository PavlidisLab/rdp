package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.UserService;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

@Controller
@CommonsLog
@Secured("ROLE_ADMIN")
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * List all users
     */
    @GetMapping(value = "/admin/users")
    public Object getAllUsers() {
        Collection<User> users = userService.findAll().stream()
                .sorted( Comparator.comparing( u -> u.getProfile().getFullName() ) )
                .collect( Collectors.toList() );
        ModelAndView view = new ModelAndView();
        view.setViewName( "admin/users" );
        view.addObject( "users", users );
        return view;
    }

    /**
     * Retrieve a user's details.
     */
    @GetMapping(value = "/admin/users/{user}")
    public Object getUser( @PathVariable User user ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        return "admin/user";
    }

    /**
     * Delete a given user.
     */
    @DeleteMapping(value = "/admin/users/{user}")
    public Object deleteUser( @PathVariable User user,
                              @RequestParam @Validated String email,
                              RedirectAttributes redirectAttributes ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        if ( user.getEmail().equals( email ) ) {
            userService.delete( user );
            return "redirect:/admin/users";
        } else {
            redirectAttributes.addFlashAttribute( "message", "User email is not correct." );
            ModelAndView view = new ModelAndView( "admin/user" );
            view.setStatus( HttpStatus.BAD_REQUEST );
            return view;
        }
    }

}
