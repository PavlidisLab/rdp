package ubc.pavlab.rdp.server.exception;

/**
 * Exception class for BioMart.
 * 
 * @author frances/jleong
 * @version $Id: BioMartServiceException.java,v 1.3 2013/05/02 23:26:38 anton Exp $
 */
public class BioMartServiceException extends ExternalDependencyException {

    private static final long serialVersionUID = 3910408548661626074L;

    public BioMartServiceException() {
    }

    public BioMartServiceException( String message ) {
        super( message );
    }
}
