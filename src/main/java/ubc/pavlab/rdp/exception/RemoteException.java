package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public final class RemoteException extends RuntimeException {
    public RemoteException() {
        super();
    }

    public RemoteException( final String message, final Throwable cause ) {
        super( message, cause );
    }

    public RemoteException( final String message ) {
        super( message );
    }

    public RemoteException( final Throwable cause ) {
        super( cause );
    }

}
