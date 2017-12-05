package ubc.pavlab.rdp.server.controller;

import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.model.User;
import gemma.gsec.util.JSONUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import ubc.pavlab.rdp.server.exception.ValidationException;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.util.Settings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mjacobson on 05/12/17.
 */
@Controller
@RemoteProxy
public class SiteController extends BaseController {

    private static Log log = LogFactory.getLog( SiteController.class );

    @Autowired
    private UserManager userManager;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @RequestMapping(value = {"", "/", "/register.html", "/home.html", "/index.html"})
    public String showRegister( ModelMap model ) {
        model.put( "ga_tracker", Settings.getAnalyticsKey() );
        model.put( "ga_domain", Settings.getAnalyticsDomain() );
        return "register";
    }

    @RequestMapping(value = "/contactSupport.html", method = RequestMethod.POST)
    public String contactSupport( HttpServletRequest request,
                                  final @RequestParam(required = false) CommonsMultipartFile attachFile ) {
        try {

            String email = userManager.getCurrentUsername();

            log.info( email + " is attempting to contact support, check debug for more information" );

            // reads form input
            // final String email = request.getParameter("email");
            final String name = request.getParameter( "name" );
            final String message = request.getParameter( "message" );

            String userAgent = request.getHeader( "User-Agent" );

            log.debug( email );
            log.debug( name );
            log.debug( message );
            log.debug( userAgent );
            log.debug( attachFile );

            String templateName = "contactSupport.vm";

            Map<String, Object> model = new HashMap<>();

            model.put( "name", name );
            model.put( "email", email );
            model.put( "userAgent", userAgent );
            model.put( "message", message );
            model.put( "boolFile", attachFile != null && !attachFile.getOriginalFilename().equals( "" ) );

            sendSupportEmail( email, "Registry Help - Contact Support", templateName, model, attachFile );
            return "success";
        } catch (Exception e) {
            // throw e;
            return "error";
        }

    }

    /**
     * Entry point for updates.
     *
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping("/editUser.html")
    public void editUser( HttpServletRequest request, HttpServletResponse response ) throws Exception {

        String password = request.getParameter( "password" );
        String passwordConfirm = request.getParameter( "passwordConfirm" );
        String oldPassword = request.getParameter( "oldPassword" );

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            /*
             * Pulling username out of security context to ensure users are logged in and can only update themselves.
             */
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            if ( !SecurityContextHolder.getContext().getAuthentication().isAuthenticated() ) {
                throw new RuntimeException( "You must be logged in to edit your profile." );
            }

            userManager.reauthenticate( username, oldPassword );

            UserDetailsImpl user = (UserDetailsImpl) userManager.loadUserByUsername( username );

            /*
             * if ( StringUtils.isNotBlank( email ) && !user.getEmail().equals( email ) ) { if ( !email.matches(
             * "/^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$/;" ) ) { // if (
             * !EmailValidator.getInstance().isValid( email ) ) { jsonText =
             * "{\"success\":false,\"message\":\"The email address does not look valid\"}"; jsonUtil.writeToResponse(
             * jsonText ); return; } user.setEmail( email ); changed = true; }
             */

            if ( password.length() >= SignupController.MIN_PASSWORD_LENGTH ) {
                if ( !StringUtils.equals( password, passwordConfirm ) ) {
                    throw new RuntimeException( "Passwords do not match." );
                }
                String encryptedPassword = passwordEncoder.encode( password );
                userManager.changePassword( oldPassword, encryptedPassword );
            } else {
                throw new RuntimeException( "Password must be at least " + SignupController.MIN_PASSWORD_LENGTH
                        + " characters in length." );
            }

            log.info( "user: (" + username + ") changed password" );
            saveMessage( request, "Changes saved." );
            jsonText = "{\"success\":true, \"message\":\"Changes saved.\"}";

        } catch (Exception e) {
            log.error( e.getLocalizedMessage(), e );
            // jsonText = jsonUtil.getJSONErrorMessage( e );
            jsonText = "{\"success\":false, \"message\":\"" + e.getLocalizedMessage() + "\"}";
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    /**
     * Begins the process for user to choose new password without logging in.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/forgotPassword.html")
    public void forgotPassword( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String email = request.getParameter( "forgotPasswordEmail" );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String txt = null;
        String jsonText = null;

        /* look up the user's information and send email */
        try {

            /* make sure the email has been set */
            if ( StringUtils.isEmpty( email ) ) {
                log.info( "Email not specified.  This is a required field." );
                jsonText = "{\"success\":false,\"message\":\"Email not specified.  This is a required field.\"}";
                return;
                // throw new RuntimeException( "Email not specified.  This is a required field." );
            }

            User user = userManager.findbyEmail( email );

            /* make sure the user exists */
            if ( user == null ) {
                log.info( "User with specified email does not exist: " + email );
                jsonText = "{\"success\":false,\"message\":\"User with specified email does not exist.\"}";
                return;
                // throw new RuntimeException( "User with specified email does not exist." );
            }

            String username = user.getUserName();

            String key = userManager.createPasswordResetToken( user );

            // String key = token.getTokenKey();

            String templateName = "forgotPassword.vm";

            Map<String, Object> model = new HashMap<>();

            String name = ((ubc.pavlab.rdp.server.model.common.auditAndSecurity.User) user).getFirstName();
            if ( StringUtils.isEmpty( name ) ) {
                name = username;
            }

            model.put( "name", name );
            model.put(
                    "confirmLink",
                    Settings.getBaseUrl() + "resetPassword.jsp?key=" + key + "&user="
                            + URLEncoder.encode( username, "UTF-8" ) );

            sendEmail( email, "Password Reset Instructions", templateName, model );

            String message = "Password reset instructions have been sent to <strong>" + email + "</strong>";

            jsonText = "{\"success\":true,\"message\":\"" + message + "\"}";

        } catch (Exception e) {
            log.error( e.getLocalizedMessage(), e );
            jsonText = "{\"success\":false,\"message\":\"" + e.getLocalizedMessage() + "\"}";
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    /**
     * Check token-user against database, changes password if all goes well.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/newPassword.html")
    public void newPassword( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String username = request.getParameter( "user" );
        String key = request.getParameter( "key" );
        String password = request.getParameter( "password" );
        String passwordConfirm = request.getParameter( "passwordConfirm" );

        JSONUtil jsonUtil = new JSONUtil( request, response );
        JSONObject json = new JSONObject();
        String jsonText = null;

        try {

            if ( StringUtils.isEmpty( password ) | StringUtils.isEmpty( passwordConfirm ) ) {
                throw new ValidationException( "Missing password." );
            }

            if ( !password.equals( passwordConfirm ) ) {
                throw new ValidationException( "Passwords do not match." );
            }

            if ( password.length() < SignupController.MIN_PASSWORD_LENGTH ) {
                throw new ValidationException( "Password must be at least " + SignupController.MIN_PASSWORD_LENGTH
                        + " characters in length" );
            }

            boolean valid = userManager.validatePasswordResetToken( username, key );

            if ( !valid ) {
                // This shouldn't happen...
                throw new RuntimeException( "Unknown problem with token." );
            }

            String pwd = passwordEncoder.encode( password );

            userManager.changePasswordForUser( username, pwd );
            userManager.invalidatePasswordResetToken( username );

            // log in automatically for convenience
            UserDetails user = userManager.loadUserByUsername( username );
            Authentication auth = new UsernamePasswordAuthenticationToken( username, password );
            SecurityContextHolder.getContext().setAuthentication( auth );

            log.info( "user: (" + username + ") reset password" );
            json.put( "success", true );
            json.put( "message", "Password successfully reset for (" + username + ")." );
            jsonText = json.toString();

        } catch (ValidationException e) {
            log.info( username + ": " + e.getLocalizedMessage() );
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText = json.toString();
        } catch (Exception e) {
            log.error( username + ": " + e.getLocalizedMessage(), e );
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText = json.toString();
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

}
