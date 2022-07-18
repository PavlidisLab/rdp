package ubc.pavlab.rdp.security.authentication;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Token-based authentication filter that lookups the 'Authorization' header and the 'auth' request parameter.
 * <p>
 * Note that the value of the 'Authorization' header takes precedence over the request parameter. The latter is not
 * recommended since it leaks the token in the URL, although it is kept for historical reasons.
 * <p>
 * This is largely inspired from {@link org.springframework.security.web.authentication.www.BasicAuthenticationFilter}.
 *
 * @author poirigui
 * @see org.springframework.security.web.authentication.www.BasicAuthenticationFilter
 */
public class TokenBasedAuthenticationFilter extends OncePerRequestFilter {

    private final RequestMatcher requestMatcher;

    private final AuthenticationManager authenticationManager;

    private final AuthenticationEntryPoint authenticationEntryPoint;

    private final AuthenticationConverter authenticationConverter = new TokenBasedAuthenticationConverter();

    /**
     * Create a new token-based authentication filter.
     *
     * @param requestMatcher           matches requests that are subject to this filter and will be challenged
     * @param authenticationManager    supplies {@link Authentication} objects from the token obtained in the request as
     *                                 a {@link TokenBasedAuthentication}
     * @param authenticationEntryPoint used when an {@link AuthenticationException} is raised either through malformed
     *                                 credentials or by the authentication manager
     */
    public TokenBasedAuthenticationFilter( RequestMatcher requestMatcher, AuthenticationManager authenticationManager, AuthenticationEntryPoint authenticationEntryPoint ) {
        this.requestMatcher = requestMatcher;
        this.authenticationManager = authenticationManager;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    public TokenBasedAuthenticationFilter( RequestMatcher requestMatcher, AuthenticationManager authenticationManager ) {
        this( requestMatcher, authenticationManager, new TokenBasedAuthenticationEntryPoint() );
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {
        if ( requestMatcher.matches( request ) ) {
            try {
                Authentication authentication = authenticationConverter.convert( request );
                if ( authentication != null ) {
                    authentication = authenticationManager.authenticate( authentication );
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication( authentication );
                    SecurityContextHolder.setContext( context );
                }
            } catch ( AuthenticationException e ) {
                authenticationEntryPoint.commence( request, response, e );
                return;
            }
        }
        filterChain.doFilter( request, response );
    }
}


