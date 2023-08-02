package ubc.pavlab.rdp.exception;

/**
 * Exception raised when the token does not match the email.
 *
 * @author poirigui
 */
public class TokenDoesNotMatchEmailException extends TokenException {

    public TokenDoesNotMatchEmailException( String message ) {
        super( message );
    }
}
