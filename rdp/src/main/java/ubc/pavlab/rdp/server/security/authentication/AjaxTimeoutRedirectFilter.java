/*
 * The rdp project
 * 
 * Copyright (c) 2015 University of British Columbia
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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.security.web.util.ThrowableCauseExtractor;
import org.springframework.web.filter.GenericFilterBean;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class AjaxTimeoutRedirectFilter extends GenericFilterBean {

    private static final Log logger = LogFactory.getLog( AjaxTimeoutRedirectFilter.class );

    private ThrowableAnalyzer throwableAnalyzer = new DefaultThrowableAnalyzer();
    private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    private int customSessionExpiredErrorCode = 901;

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException,
            ServletException {
        try {
            chain.doFilter( request, response );

            logger.debug( "Chain processed normally" );
        } catch ( IOException ex ) {
            throw ex;
        } catch ( Exception ex ) {
            Throwable[] causeChain = throwableAnalyzer.determineCauseChain( ex );
            RuntimeException ase = ( AuthenticationException ) throwableAnalyzer.getFirstThrowableOfType(
                    AuthenticationException.class, causeChain );

            if ( ase == null ) {
                ase = ( AccessDeniedException ) throwableAnalyzer.getFirstThrowableOfType( AccessDeniedException.class,
                        causeChain );
            }

            if ( ase != null ) {
                if ( ase instanceof AuthenticationException ) {
                    throw ase;
                } else if ( ase instanceof AccessDeniedException ) {

                    if ( authenticationTrustResolver.isAnonymous( SecurityContextHolder.getContext()
                            .getAuthentication() ) ) {
                        // logger.info( "User session expired or not logged in yet" );
                        String ajaxHeader = ( ( HttpServletRequest ) request ).getHeader( "X-Requested-With" );

                        if ( "XMLHttpRequest".equals( ajaxHeader ) ) {
                            logger.info( "Ajax call detected, send {" + this.customSessionExpiredErrorCode
                                    + "} error code" );
                            HttpServletResponse resp = ( HttpServletResponse ) response;
                            resp.sendError( this.customSessionExpiredErrorCode );
                        } else {
                            // logger.info( "Redirect to login page" );
                            throw ase;
                        }
                    } else {
                        throw ase;
                    }
                }
            }

        }
    }

    private static final class DefaultThrowableAnalyzer extends ThrowableAnalyzer {
        /**
         * @see org.springframework.security.web.util.ThrowableAnalyzer#initExtractorMap()
         */
        @Override
        protected void initExtractorMap() {
            super.initExtractorMap();

            registerExtractor( ServletException.class, new ThrowableCauseExtractor() {
                @Override
                public Throwable extractCause( Throwable throwable ) {
                    ThrowableAnalyzer.verifyThrowableHierarchy( throwable, ServletException.class );
                    return ( ( ServletException ) throwable ).getRootCause();
                }
            } );
        }

    }

    public void setCustomSessionExpiredErrorCode( int customSessionExpiredErrorCode ) {
        this.customSessionExpiredErrorCode = customSessionExpiredErrorCode;
    }
}
