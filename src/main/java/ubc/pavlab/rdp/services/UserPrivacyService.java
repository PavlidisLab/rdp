package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.text.MessageFormat;

@Service
@CommonsLog
public class UserPrivacyService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationSettings applicationSettings;

    public boolean checkUserCanSee( User user, UserContent content ) {
        return checkUserCanSeeOtherUserContentWithPrivacyLevel( user, content.getOwner().orElse( null ), content.getEffectivePrivacyLevel() );
    }

    public boolean checkUserCanSearch( User user, boolean international ) {
        // international search is not enabled
        if ( international && !applicationSettings.getIsearch().isEnabled() ) {
            return false;
        }
        return ( user == null && applicationSettings.getPrivacy().isPublicSearch() ) // Search is public, even for unregistered users
                || ( user != null && applicationSettings.getPrivacy().isRegisteredSearch() ) // Search is registered and there is user logged
                || ( user != null && user.getRoles().contains( getAdminRole() ) ) // User is admin
                || ( user != null && user.getRoles().contains( getServiceAccountRole() ) ); // user is a service account
    }

    public boolean checkCurrentUserCanSearch( boolean international ) {
        return checkUserCanSearch( userService.findCurrentUser(), international );
    }

    public boolean checkUserCanUpdate( User user, UserContent userContent ) {
        // only admins or rightful owner can update user content
        return user != null && ( user.getRoles().contains( getAdminRole() ) || userContent.getOwner().filter( user::equals ).isPresent() );
    }

    private boolean checkUserCanSeeOtherUserContentWithPrivacyLevel( User currentUser, User otherUser, PrivacyLevelType privacyLevel ) {
        // Never show the remote admin profile (or accidental null users)
        if ( otherUser == null || ( applicationSettings.getIsearch() != null && isRemoteSearchUser( otherUser ) ) ) {
            return false;
        }

        // never show unverified accounts
        if ( !otherUser.isEnabled() ) {
            return false;
        }

        Profile profile = otherUser.getProfile();

        if ( profile == null || profile.getPrivacyLevel() == null ) {
            log.error( MessageFormat.format( "User without a profile, privacy levels or sharing set: {0}", otherUser ) );
            return false;
        }


        // Either the user is looking at himself, or the user is public, or shared with registered users - check for any logged-in user, or private - check for admin; If logged-in user is admin, we have to
        // check whether this user is the designated actor for the authenticated remote search, in which case we have to check for remote search privileges on the user.
        return otherUser.equals( currentUser ) // User is looking at himself
                || ( privacyLevel == PrivacyLevelType.PUBLIC ) // Data is public
                || ( privacyLevel == PrivacyLevelType.SHARED && currentUser != null && !isRemoteSearchUser( currentUser ) )// data is accessible for registerd users and there is a user logged in who is not the remote admin
                || ( privacyLevel == PrivacyLevelType.PRIVATE && currentUser != null && isAdminOrServiceAccount( currentUser ) && !isRemoteSearchUser( currentUser ) ) // data is private and there is an admin (or service account) logged in who is not the remote search user
                || ( profile.isShared() && currentUser != null && isAdminOrServiceAccount( currentUser ) && isRemoteSearchUser( currentUser ) ); // data is designated as remotely shared and there is an admin (or service account) logged in who is the remote search user
    }

    private boolean isAdminOrServiceAccount( User user ) {
        return user.getRoles().contains( getAdminRole() ) || user.getRoles().contains( getServiceAccountRole() );
    }

    /**
     * Check if a given user can see another user's gene list.
     */
    public boolean checkCurrentUserCanSeeGeneList( User otherUser ) {
        User currentUser = userService.findCurrentUser();
        return checkUserCanSee( currentUser, otherUser )
                && ( !otherUser.getProfile().isHideGenelist()
                || ( currentUser != null && currentUser.equals( otherUser ) )
                || ( currentUser != null && currentUser.getRoles().contains( getAdminRole() ) ) );
    }

    @Cacheable(value = "ubc.pavlab.rdp.model.Role.byRole", key = "'ROLE_ADMIN'")
    public Role getAdminRole() {
        return roleRepository.findByRole( "ROLE_ADMIN" );
    }

    @Cacheable(value = "ubc.pavlab.rdp.model.Role.byRole", key = "'ROLE_SERVICE_ACCOUNT'")
    public Role getServiceAccountRole() {
        return roleRepository.findByRole( "ROLE_SERVICE_ACCOUNT" );
    }

    private boolean isRemoteSearchUser( User user ) {
        return userService.getRemoteSearchUser().filter( user::equals ).isPresent();
    }
}
