package ubc.pavlab.rdp.server.exception;

/**
 * author: anton date: 02/05/13
 */
public class ExternalDependencyException extends Exception {

    private static final long serialVersionUID = 7197580118891737230L;

    ExternalDependencyException() {
    }

    ExternalDependencyException( String message ) {
        super( message );
    }
}
