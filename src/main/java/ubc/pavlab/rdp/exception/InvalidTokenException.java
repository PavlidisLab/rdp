package ubc.pavlab.rdp.exception;

/**
 * Exception raised when a token is malformed.
 *
 * @author poirigui
 */
public class InvalidTokenException extends TokenException {
    public InvalidTokenException( String message ) {
        super( message );
    }
}
