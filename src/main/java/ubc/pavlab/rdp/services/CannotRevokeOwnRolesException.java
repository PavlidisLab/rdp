package ubc.pavlab.rdp.services;

/**
 * Exception raised when a user attempt to revoke its own roles.
 * <p>
 * This is now allowed as it might lead to the user locking him/herself out.
 *
 * @author poirigui
 */
public class CannotRevokeOwnRolesException extends RoleException {

    public CannotRevokeOwnRolesException( String message ) {
        super( message );
    }
}
