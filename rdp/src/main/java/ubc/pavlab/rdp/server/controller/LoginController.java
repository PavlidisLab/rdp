package ubc.pavlab.rdp.server.controller;

import gemma.gsec.util.JSONUtil;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.security.authentication.UserManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RemoteProxy
public class LoginController {

    private static Log log = LogFactory.getLog( LoginController.class );

    @Autowired
    private UserManager userManager;

    @RequestMapping(value = "/ajaxLoginCheck.html")
    public void ajaxLoginCheck( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = "{\"success\":false}";
        String userName = null;

        try {

            if ( userManager.loggedIn() ) {
                userName = userManager.getCurrentUsername();
                log.info( userName + " has logged in." );
                jsonText = "{\"success\":true,\"user\":\"" + userName + "\",\"isAdmin\":\""
                        + SecurityUtil.isUserAdmin() + "\"}";
            } else {
                jsonText = "{\"success\":false}";
            }
        } catch (Exception e) {

            log.error( e, e );
            jsonText = jsonUtil.getJSONErrorMessage( e );
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

    @RequestMapping("/keep_alive.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;

        if ( !SecurityUtil.isUserLoggedIn() ) {
            jsonText = "{success:false}";

        } else {
            jsonText = "{success:true}";
        }

        JSONUtil jsonUtil = new JSONUtil( request, response );

        jsonUtil.writeToResponse( jsonText );

    }

    @RequestMapping("/login.html")
    public String showLogin( ModelMap model ) {
        return "login";
    }

}