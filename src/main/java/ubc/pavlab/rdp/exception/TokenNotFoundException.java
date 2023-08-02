package ubc.pavlab.rdp.exception;

/**
 * Exception raised when a token is not found.
 *
 * @author poirigui
 */
public class TokenNotFoundException extends TokenException {

    public TokenNotFoundException( String message ) {
        super( message );
    }
}
