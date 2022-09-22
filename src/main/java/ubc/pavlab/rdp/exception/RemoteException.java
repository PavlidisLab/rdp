package ubc.pavlab.rdp.exception;

/**
 * Created by mjacobson on 22/01/18.
 */
public class RemoteException extends Exception {

    public RemoteException( final String message ) {
        super( message );
    }

    public RemoteException( final String message, Throwable e ) {
        super( message, e );
    }
}
