package ubc.pavlab.rdp.security;

import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.Token;

public interface SecureTokenChallenge<T> {

    /**
     * @param token the token being challenged
     * @throws TokenException if the challenge fails
     */
    void challenge( Token token, T object ) throws TokenException;

    /**
     * Indicate if the challenge supports the given class.
     */
    boolean supports( Class<?> clazz );
}
