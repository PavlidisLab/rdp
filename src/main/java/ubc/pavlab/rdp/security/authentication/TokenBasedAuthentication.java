package ubc.pavlab.rdp.security.authentication;

import org.springframework.security.authentication.AbstractAuthenticationToken;

public class TokenBasedAuthentication extends AbstractAuthenticationToken {

    private final String token;
    private final String secret;

    public TokenBasedAuthentication( String token, String secret ) {
        super( null );
        this.token = token;
        this.secret = secret;
    }

    @Override
    public Object getCredentials() {
        return secret;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }
}
