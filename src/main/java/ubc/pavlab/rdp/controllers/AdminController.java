package ubc.pavlab.rdp.controllers;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.UserService;

@RestController
@Log
public class AdminController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/admin/user/{id}/delete", method = RequestMethod.GET)
    public String deleteUser( @PathVariable int id ) {
        User user = userService.findUserById( id );
        if ( user != null ) {
            userService.delete( user );
        }
        return "Deleted.";
    }

}
