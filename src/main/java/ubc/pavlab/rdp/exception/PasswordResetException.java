package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public final class PasswordResetException extends RuntimeException {
    public PasswordResetException() {
        super();
    }

    public PasswordResetException( final String message, final Throwable cause) {
        super(message, cause);
    }

    public PasswordResetException( final String message) {
        super(message);
    }

    public PasswordResetException( final Throwable cause) {
        super(cause);
    }

}
