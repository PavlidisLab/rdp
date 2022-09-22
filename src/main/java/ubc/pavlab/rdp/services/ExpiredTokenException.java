package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.exception.TokenException;

public class ExpiredTokenException extends TokenException {
    public ExpiredTokenException( String message ) {
        super( message );
    }
}
