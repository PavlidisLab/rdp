package ubc.pavlab.rdp.exception;

/**
 * Exception raised when the MAC signature of a token is invalid.
 */
public class InvalidTokenSignatureException extends TokenException {

    public InvalidTokenSignatureException( String message ) {
        super( message );
    }
}
