package ubc.pavlab.rdp.server.controller;

import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RemoteProxy
public class RegisterController {

    @RequestMapping(value = { "", "/", "/register.html", "/home.html", "/index.html" })
    public String showRegister( ModelMap model ) {
        return "register";
    }

    /**
     * @RequestMapping("/keep_alive.html") public void loadUser( HttpServletRequest request, HttpServletResponse response
     *                                     ) throws IOException { String jsonText = null; if (
     *                                     !SecurityServiceImpl.isUserLoggedIn() ) { jsonText = "{success:false}"; }
     *                                     else { jsonText = "{success:true}"; } JSONUtil jsonUtil = new JSONUtil(
     *                                     request, response ); jsonUtil.writeToResponse( jsonText ); }
     */

}