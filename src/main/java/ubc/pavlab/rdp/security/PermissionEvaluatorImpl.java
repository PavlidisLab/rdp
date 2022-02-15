package ubc.pavlab.rdp.security;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;

import java.io.Serializable;

@Component
@CommonsLog
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    @Autowired
    private UserService userService;

    @Autowired
    private PrivacyService privacyService;

    @Override
    public boolean hasPermission( Authentication authentication, Object targetDomainObject, Object permission ) {
        User user = authentication.getPrincipal().equals( "anonymousUser" ) ? null : userService.findUserByIdNoAuth( ( (UserPrinciple) authentication.getPrincipal() ).getId() );
        if ( permission.equals( "search" ) ) {
            return privacyService.checkUserCanSearch( user, false );
        } else if ( permission.equals( "international-search" ) ) {
            return privacyService.checkUserCanSearch( user, true );
        } else if ( permission.equals( "read" ) && targetDomainObject == null ) {
            // null objects must be let through, so they can be handled as a 404 if necessary
            return true;
        } else if ( permission.equals( "read" ) && targetDomainObject instanceof UserContent ) {
            return privacyService.checkUserCanSee( user, (UserContent) targetDomainObject );
        } else if ( permission.equals( "update" ) && targetDomainObject instanceof UserContent ) {
            return privacyService.checkUserCanUpdate( user, (UserContent) targetDomainObject );
        } else {
            throw new UnsupportedOperationException( "Permission " + permission + " is not supported." );
        }

    }

    @Override
    public boolean hasPermission( Authentication authentication, Serializable targetId, String targetType, Object permission ) {
        throw new UnsupportedOperationException( "Permission on target ID is not supported." );
    }
}
