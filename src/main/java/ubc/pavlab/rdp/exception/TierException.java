package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public final class TierException extends Exception {
    public TierException() {
        super();
    }

    public TierException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public TierException( final String message ) {
        super( message );
    }

    public TierException( final Throwable cause ) {
        super( cause );
    }

}
