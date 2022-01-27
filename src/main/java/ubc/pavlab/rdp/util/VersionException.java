package ubc.pavlab.rdp.util;

public class VersionException extends RuntimeException {

    public VersionException( String message ) {
        super( message );
    }

    public VersionException( String message, Throwable cause ) {
        super( message, cause );
    }
}
