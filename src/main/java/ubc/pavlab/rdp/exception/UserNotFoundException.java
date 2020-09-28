package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public final class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super();
    }

    public UserNotFoundException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public UserNotFoundException( final String message ) {
        super( message );
    }

    public UserNotFoundException( final Throwable cause ) {
        super( cause );
    }

}
