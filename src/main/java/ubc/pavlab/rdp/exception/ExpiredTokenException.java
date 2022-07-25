package ubc.pavlab.rdp.exception;

/**
 * Exception raised when a token is expired.
 *
 * @author poirigui
 */
public class ExpiredTokenException extends TokenException {
    public ExpiredTokenException( String message ) {
        super( message );
    }
}
