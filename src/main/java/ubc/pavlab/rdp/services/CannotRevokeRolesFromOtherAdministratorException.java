package ubc.pavlab.rdp.services;

/**
 * Exception raised when a user attempt to revoke roles from other administrators.
 */
public class CannotRevokeRolesFromOtherAdministratorException extends RoleException {
    public CannotRevokeRolesFromOtherAdministratorException( String s ) {
        super( s );
    }
}
