package ubc.pavlab.rdp.server.controller;

import gemma.gsec.util.JSONUtil;
import gemma.gsec.util.SecurityUtil;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;

@Controller
@RemoteProxy
public class LoginController {

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
    
    /**
     * @param request
     * @param recatpchaPvtKey
     * @return
     */
    @RequestMapping("/verifyCaptcha.html")
    public boolean validateCaptcha( HttpServletRequest request, String recaptchaPvtKey ) {
        /*
         // https://developers.google.com/recaptcha/docs/java
         
        String remoteAddr = request.getRemoteAddr();
        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
        reCaptcha.setPrivateKey("your_private_key");

        String challenge = request.getParameter("recaptcha_challenge_field");
        String uresponse = request.getParameter("recaptcha_response_field");
        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, uresponse);

        if (reCaptchaResponse.isValid()) {
          out.print("Answer was entered correctly!");
        } else {
          out.print("Answer is wrong");
        }
         */
        String rcChallenge = request.getParameter( "recaptcha_challenge_field" );
        String rcResponse = request.getParameter( "recaptcha_response_field" );

        String remoteAddr = request.getRemoteAddr();
        ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
        reCaptcha.setPrivateKey( recaptchaPvtKey );
        ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer( remoteAddr, rcChallenge, rcResponse );
        return reCaptchaResponse.isValid();
    }

    @RequestMapping("/login.html")
    public String showLogin( ModelMap model ) {
        return "login";
    }

}