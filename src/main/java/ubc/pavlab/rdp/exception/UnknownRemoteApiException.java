package ubc.pavlab.rdp.exception;

import lombok.Getter;

import java.net.URI;

/**
 * Exception raised when a remote host is not a known partner API.
 */
@Getter
public class UnknownRemoteApiException extends RemoteException {

    private final URI remoteHost;

    public UnknownRemoteApiException( URI remoteHost ) {
        super( String.format( "Unknown remote API %s.", remoteHost.getRawAuthority() ) );
        this.remoteHost = remoteHost;
    }
}
