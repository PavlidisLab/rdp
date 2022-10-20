package ubc.pavlab.rdp.security.authentication;

import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.Assert;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.ExpiredTokenException;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

/**
 * @author poirigui
 */
public class TokenBasedAuthenticationManager implements AuthenticationManager, AuthenticationProvider {

    private final UserService userService;

    private final ApplicationSettings applicationSettings;

    public TokenBasedAuthenticationManager( UserService userService, ApplicationSettings applicationSettings ) {
        this.userService = userService;
        this.applicationSettings = applicationSettings;
        Assert.isTrue( applicationSettings.getIsearch().getAuthTokens().isEmpty() || applicationSettings.getIsearch().getUserId() != null,
                "The remote search user is not configured correctly: there are auth tokens configured, no user ID is specified." );
        Assert.isTrue( applicationSettings.getIsearch().getUserId() == null || userService.getRemoteSearchUser().isPresent(),
                String.format( "The remote search user is not configured correctly: no user can be found with ID %d.", applicationSettings.getIsearch().getUserId() ) );
    }

    @Override
    public Authentication authenticate( Authentication authentication ) throws AuthenticationException {
        String authToken = (String) authentication.getCredentials();
        User u;
        if ( applicationSettings.getIsearch().getAuthTokens().contains( authToken ) ) {
            // remote admin authentication
            u = userService.getRemoteSearchUser()
                    .orElseThrow( () -> new InternalAuthenticationServiceException(
                            String.format( "The remote search user is not configured correctly: no user can be found with ID %d.",
                                    applicationSettings.getIsearch().getUserId() ) ) );
        } else {
            // authentication via access token
            try {
                u = userService.findUserByAccessTokenNoAuth( authToken );
            } catch ( ExpiredTokenException e ) {
                throw new CredentialsExpiredException( "API token is expired.", e );
            } catch ( TokenException e ) {
                throw new BadCredentialsException( "API token is invalid.", e );
            }
        }

        if ( u == null ) {
            throw new BadCredentialsException( "No user associated to the provided API token." );
        }

        return new UsernamePasswordAuthenticationToken( new UserPrinciple( u ), authToken, new UserPrinciple( u ).getAuthorities() );
    }

    @Override
    public boolean supports( Class<?> authentication ) {
        return TokenBasedAuthentication.class.isAssignableFrom( authentication );
    }
}
