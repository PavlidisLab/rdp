package ubc.pavlab.rdp.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private final String token;

    public TokenBasedAuthentication( String token ) {
        super( null );
        this.token = token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return null;
    }
}
