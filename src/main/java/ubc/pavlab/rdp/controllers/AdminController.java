package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.UserService;

@RestController
@CommonsLog
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/admin/user/{id}/delete", method = RequestMethod.GET)
    public String deleteUser( @PathVariable int id ) {
        User user = userService.findUserById( id );
        if ( user != null ) {
            userService.delete( user );
        }
        return "Deleted.";
    }

}
