/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
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

package ubc.pavlab.rdp.server.security.authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import ubc.pavlab.rdp.server.util.JSONUtil;

/**
 * Strategy used to handle a failed user authentication if it is a ajax style login (ajaxLoginTrue parameter = true)
 * then no redirect happens and a some JSON is sent to the client if the request is not ajax style then the default
 * redirection takes place
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AjaxAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception ) throws ServletException, IOException {

        String ajaxLoginTrue = request.getParameter( "ajaxLoginTrue" );

        if ( ajaxLoginTrue != null && ajaxLoginTrue.equals( "true" ) ) {

            JSONUtil jsonUtil = new JSONUtil( request, response );
            String jsonText = null;

            this.setRedirectStrategy( new RedirectStrategy() {

                @Override
                public void sendRedirect( HttpServletRequest re, HttpServletResponse res, String s ) {
                    // do nothing, no redirect to make it work with extjs

                }
            } );

            super.onAuthenticationFailure( request, response, exception );
            JSONObject json = new JSONObject();
            json.put( "success", false );

            if ( exception.getClass().isAssignableFrom( BadCredentialsException.class ) ) {
                json.put( "message", "<strong>Warning!</strong> Login email/password incorrect." );
            } else if ( exception.getClass().isAssignableFrom( LockedException.class ) ) {
                json.put( "message",
                        "Your account has not been activated, please click the confirmation link that was e-mailed to you upon registration." );
            } else {
                json.put( "message", "Login Failed" );
            }
            jsonText = json.toString();
            jsonUtil.writeToResponse( jsonText );

        }

        else {

            this.setRedirectStrategy( new DefaultRedirectStrategy() );

            super.onAuthenticationFailure( request, response, exception );

        }

    }

}
