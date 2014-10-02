/*
 * The RDP project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubc.pavlab.rdp.server.controller;

import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.model.User;
import gemma.gsec.util.JSONUtil;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.PasswordResetToken;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.util.Settings;

/**
 * Controller to edit profile of users.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id: UserFormMultiActionController.java,v 1.22 2014/06/19 21:42:36 ptan Exp $
 */
@Controller
public class UserFormMultiActionController extends BaseController {

    public static final int MIN_PASSWORD_LENGTH = 6;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

            UserDetailsImpl user = ( UserDetailsImpl ) userManager.loadUserByUsername( username );

            /*
             * if ( StringUtils.isNotBlank( email ) && !user.getEmail().equals( email ) ) { if ( !email.matches(
             * "/^(\\w+)([-+.][\\w]+)*@(\\w[-\\w]*\\.){1,5}([A-Za-z]){2,4}$/;" ) ) { // if (
             * !EmailValidator.getInstance().isValid( email ) ) { jsonText =
             * "{\"success\":false,\"message\":\"The email address does not look valid\"}"; jsonUtil.writeToResponse(
             * jsonText ); return; } user.setEmail( email ); changed = true; }
             */

            if ( password.length() >= MIN_PASSWORD_LENGTH ) {
                if ( !StringUtils.equals( password, passwordConfirm ) ) {
                    throw new RuntimeException( "Passwords do not match." );
                }
                String encryptedPassword = passwordEncoder.encodePassword( password, username );
                userManager.changePassword( oldPassword, encryptedPassword );
            } else {
                throw new RuntimeException( "Password must be at least " + MIN_PASSWORD_LENGTH
                        + " characters in length." );
            }

            log.info( "user: (" + username + ") changed password" );
            saveMessage( request, "Changes saved." );
            jsonText = "{\"success\":true, \"message\":\"Changes saved.\"}";

        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            // jsonText = jsonUtil.getJSONErrorMessage( e );
            jsonText = "{\"success\":false, \"message\":\"" + e.getLocalizedMessage() + "\"}";
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    /**
     * AJAX entry point. Loads a user.
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadUser.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            log.error( "User not authenticated.  Cannot populate user data." );
            return;
        }

        Object o = authentication.getPrincipal();
        String username = null;

        if ( o instanceof UserDetails ) {
            username = ( ( UserDetails ) o ).getUsername();
        } else {
            username = o.toString();
        }

        User user = userManager.findByUserName( username );

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( user == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No user with name " + username + "\"}";
            } else {
                // jsonText = "{\"success\":true, \"data\":{\"username\":" + "\"" + username + "\"" + ",\"email\":" +
                // "\""
                // + user.getEmail() + "\"" + "}}";
                JSONObject json = new JSONObject( user );
                jsonText = "{\"success\":true, \"data\":" + json.toString() + "}";
                // log.debug( "Success! json=" + jsonText );
            }

        } catch ( Exception e ) {
            jsonText = "{\"success\":false,\"message\":\"" + e.getLocalizedMessage() + "\"}";
        } finally {
            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
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
                throw new RuntimeException( "Email not specified.  This is a required field." );
            }

            User user = userManager.findbyEmail( email );

            /* make sure the user exists */
            if ( user == null ) {
                throw new RuntimeException( "User with specified email does not exist." );
            }

            String username = user.getUserName();

            PasswordResetToken token = userManager.createPasswordResetToken( user );

            String key = token.getTokenKey();

            String templateName = "forgotPassword.vm";

            Map<String, Object> model = new HashMap<>();

            String name = ( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user ).getFirstName();
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

        } catch ( Exception e ) {
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
                throw new RuntimeException( "Missing password." );
            }

            if ( !password.equals( passwordConfirm ) ) {
                throw new RuntimeException( "Passwords do not match." );
            }

            if ( password.length() < MIN_PASSWORD_LENGTH ) {
                throw new RuntimeException( "Password must be at least " + MIN_PASSWORD_LENGTH
                        + " characters in length" );
            }

            boolean valid = userManager.validatePasswordResetToken( username, key );

            if ( !valid ) {
                // This shouldn't happen...
                throw new RuntimeException( "Unknown problem with token." );
            }

            String pwd = passwordEncoder.encodePassword( password, username );

            userManager.changePasswordForUser( username, pwd );
            userManager.invalidatePasswordResetToken( username );

            log.info( "user: (" + username + ") reset password" );
            json.put( "success", true );
            json.put( "message", "Password successfully reset for (" + username + ")." );
            jsonText = json.toString();

        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText = json.toString();
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

}