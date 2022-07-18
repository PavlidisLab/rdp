package ubc.pavlab.rdp.security.authentication;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Entry point for a token-based authentication.
 * <p>
 * This entry points produces a 'WWW-Authenticate: Bearer' header and emits the {@link AuthenticationException} message
 * in the response in 'text/plain'.
 *
 * @author poirigui
 * @see org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
 */
public class TokenBasedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence( HttpServletRequest request, HttpServletResponse response, AuthenticationException authException ) throws IOException {
        response.setStatus( HttpStatus.UNAUTHORIZED.value() );
        response.addHeader( "WWW-Authenticate", "Bearer" );
        response.setContentType( MediaType.TEXT_PLAIN_VALUE );
        response.getWriter().print( authException.getMessage() );
        response.getWriter().flush(); /* commits the response */
    }
}
