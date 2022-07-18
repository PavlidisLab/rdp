package ubc.pavlab.rdp.security.authentication;

import org.springframework.security.web.authentication.AuthenticationConverter;

import javax.servlet.http.HttpServletRequest;

/**
 * Parses a {@link HttpServletRequest} to extract a {@link TokenBasedAuthentication} object from the 'Autorization'
 * header or 'auth' request parameter.
 *
 * @author poirigui
 */
class TokenBasedAuthenticationConverter implements AuthenticationConverter {

    @Override
    public final TokenBasedAuthentication convert( HttpServletRequest request ) throws IllegalArgumentException {
        String authorizationHeader = request.getHeader( "Authorization" );
        String authToken = request.getParameter( "auth" );
        if ( authToken == null && authorizationHeader != null ) {
            String[] pieces = authorizationHeader.split( " ", 2 );
            if ( pieces[0].equalsIgnoreCase( "Bearer" ) ) {
                if ( pieces.length == 2 ) {
                    authToken = pieces[1];
                } else {
                    throw new IllegalArgumentException( "Cannot parse Authorization header, should be 'Bearer <api_key>'." );
                }
            } else {
                return null; /* unsupported authentication scheme */
            }
        }
        return authToken != null ? new TokenBasedAuthentication( authToken ) : null;
    }
}
