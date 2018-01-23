package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public final class TokenException extends RuntimeException {
    public TokenException() {
        super();
    }

    public TokenException( final String message, final Throwable cause) {
        super(message, cause);
    }

    public TokenException( final String message) {
        super(message);
    }

    public TokenException( final Throwable cause) {
        super(cause);
    }

}
