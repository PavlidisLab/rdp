package ubc.pavlab.rdp.security;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;

import java.io.Serializable;

@Component
@CommonsLog
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    @Autowired
    UserService userService;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PrivacyService privacyService;

    @Override
    public boolean hasPermission( Authentication authentication, Object targetDomainObject, Object permission ) {
        User user = authentication.getPrincipal().equals( "anonymousUser" ) ? null : userService.findUserByIdNoAuth( ( (UserPrinciple) authentication.getPrincipal() ).getId() );
        if ( permission.equals( "search" ) ) {
            return privacyService.checkUserCanSearch( user, false );
        } else if ( permission.equals( "international-search" ) ) {
            return privacyService.checkUserCanSearch( user, true );
        } else if ( targetDomainObject instanceof UserContent ) {
            if ( permission.equals( "search" ) ) {
                return privacyService.checkUserCanSearch( user, false );
            } else if ( permission.equals( "read" ) ) {
                return privacyService.checkUserCanSee( user, (UserContent) targetDomainObject );
            } else if ( permission.equals( "update" ) ) {
                return privacyService.checkUserCanUpdate( user, (UserContent) targetDomainObject );
            }
        }

        throw new UnsupportedOperationException( "" );
    }

    @Override
    public boolean hasPermission( Authentication authentication, Serializable targetId, String targetType, Object permission ) {
        throw new UnsupportedOperationException( "" );
    }
}
