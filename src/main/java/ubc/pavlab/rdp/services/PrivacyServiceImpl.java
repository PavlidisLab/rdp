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

/**
 * Logic regarding privacy
 */
@Service("privacyService")
@CommonsLog
public class PrivacyServiceImpl implements PrivacyService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Override
    public boolean checkUserCanSee( User user, UserContent content ) {
        return checkUserCanSeeOtherUserContentWithPrivacyLevel( user, content.getOwner().orElse( null ), content.getEffectivePrivacyLevel() );
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPrivacyLevelEnabled( PrivacyLevelType privacyLevel ) {
        return applicationSettings.getPrivacy().getEnabledLevels().contains( privacyLevel.toString() ) &&
                privacyLevel.ordinal() >= applicationSettings.getPrivacy().getMinLevel();
    }

    @Override
    public boolean isGenePrivacyLevelEnabled( PrivacyLevelType privacyLevel ) {
        return isPrivacyLevelEnabled( privacyLevel ) &&
                applicationSettings.getPrivacy().getEnabledGeneLevels().contains( privacyLevel.toString() );
    }

    @Override
    public PrivacyLevelType getDefaultPrivacyLevel() {
        return PrivacyLevelType.values()[applicationSettings.getPrivacy().getDefaultLevel()];
    }

    @Override
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

    @Override
    public boolean checkCurrentUserCanSearch( boolean international ) {
        return checkUserCanSearch( userService.findCurrentUser(), international );
    }

    @Override
    public boolean checkUserCanUpdate( User user, UserContent userContent ) {
        // only admins or rightful owner can update user content
        return user.getRoles().contains( getAdminRole() ) || userContent.getOwner().map( u -> u.equals( user ) ).orElse( false );
    }

    private boolean checkUserCanSeeOtherUserContentWithPrivacyLevel( User currentUser, User otherUser, PrivacyLevelType privacyLevel ) {
        // Never show the remote admin profile (or accidental null users)
        if ( otherUser == null || ( applicationSettings.getIsearch() != null && otherUser.getId().equals( applicationSettings.getIsearch().getUserId() ) ) ) {
            return false;
        }

        Profile profile = otherUser.getProfile();

        if ( profile == null || profile.getPrivacyLevel() == null || profile.getShared() == null ) {
            log.error( MessageFormat.format( "User without a profile, privacy levels or sharing set: {0}", otherUser ) );
            return false;
        }

        // Either the user is looking at himself, or the user is public, or shared with registered users - check for any logged-in user, or private - check for admin; If logged-in user is admin, we have to
        // check whether this user is the designated actor for the authenticated remote search, in which case we have to check for remote search privileges on the user.
        return otherUser.equals( currentUser ) // User is looking at himself
                || ( privacyLevel.equals( PrivacyLevelType.PUBLIC ) ) // Data is public
                || ( privacyLevel.equals( PrivacyLevelType.SHARED ) && currentUser != null && !currentUser.getId().equals( applicationSettings.getIsearch().getUserId() ) )// data is accessible for registerd users and there is a user logged in who is not the remote admin
                || ( privacyLevel.equals( PrivacyLevelType.PRIVATE ) && currentUser != null && currentUser.getRoles().contains( getAdminRole() ) && !currentUser.getId().equals( applicationSettings.getIsearch().getUserId() ) )// data is private and there is an admin logged in who is not the remote admin
                || ( privacyLevel.equals( PrivacyLevelType.PRIVATE ) && currentUser != null && currentUser.getRoles().contains( getServiceAccountRole() ) && !currentUser.getId().equals( applicationSettings.getIsearch().getUserId() ) ) // user is a service account
                || ( profile.getShared() && currentUser != null && currentUser.getRoles().contains( getAdminRole() ) && currentUser.getId().equals( applicationSettings.getIsearch().getUserId() ) ); // data is designated as remotely shared and there is an admin logged in who is the remote admin
    }

    @Cacheable
    public Role getAdminRole() {
        return roleRepository.findByRole( "ROLE_ADMIN" );
    }

    @Cacheable
    public Role getServiceAccountRole() {
        return roleRepository.findByRole( "ROLE_SERVICE_ACCOUNT" );
    }
}
