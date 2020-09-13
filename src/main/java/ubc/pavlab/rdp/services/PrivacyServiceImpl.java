package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

/**
 * Logic regarding privacy
 */
@Service("privacyService")
@CommonsLog
public class PrivacyServiceImpl implements PrivacyService {

    @Autowired
    ApplicationSettings applicationSettings;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserService userService;

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

    private static Role roleAdmin = null;

    private boolean checkUserCanSeeOtherUserContentWithPrivacyLevel( User currentUser, User otherUser, PrivacyLevelType privacyLevel ) {
        if ( roleAdmin == null ) {
            roleAdmin = roleRepository.findByRole( "ROLE_ADMIN" );
        }

        // Never show the remote admin profile (or accidental null users)
        if ( otherUser == null || ( applicationSettings.getIsearch() != null && otherUser.getId()
                .equals( applicationSettings.getIsearch().getUserId() ) ) ) {
            return false;
        }

        Profile profile = otherUser.getProfile();

        if ( profile == null || profile.getPrivacyLevel() == null || profile.getShared() == null ) {
            log.error( "!! User without a profile, privacy levels or sharing set: " + otherUser.getId() + " / " + otherUser
                    .getEmail() );
            return false;
        }

        // Either the user is looking at himself, or the user is public, or shared with registered users - check for any logged-in user, or private - check for admin; If logged-in user is admin, we have to
        // check whether this user is the designated actor for the authenticated remote search, in which case we have to check for remote search privileges on the user.
        return otherUser.equals( currentUser ) // User is looking at himself
                || ( privacyLevel.equals( PrivacyLevelType.PUBLIC ) ) // Data is public
                || ( privacyLevel.equals( PrivacyLevelType.SHARED ) && currentUser != null && !currentUser
                .getId().equals( applicationSettings.getIsearch().getUserId() ) )
                // data is accessible for registerd users and there is a user logged in who is not the remote admin
                || ( privacyLevel.equals( PrivacyLevelType.PRIVATE ) && currentUser != null && currentUser
                .getRoles().contains( roleAdmin ) && !currentUser.getId()
                .equals( applicationSettings.getIsearch().getUserId() ) )
                // data is private and there is an admin logged in who is not the remote admin
                || ( profile.getShared() && currentUser != null && currentUser.getRoles().contains( roleAdmin )
                && currentUser.getId().equals( applicationSettings.getIsearch()
                .getUserId() ) ); // data is designated as remotely shared and there is an admin logged in who is the remote admin
    }

    public boolean checkUserCanSearch( User user, boolean international ) {
        Role adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        return ( user == null && applicationSettings.getPrivacy().isPublicSearch() // Search is public, even for unregistered users
                || ( user != null && applicationSettings.getPrivacy().isRegisteredSearch() ) // Search is registered and there is user logged
                || ( user != null && adminRole != null && user.getRoles().contains( adminRole ) ) ) // User is admin
                && ( !international || applicationSettings.getIsearch().isEnabled() ); // International search enabled
    }

    @Override
    public boolean checkCurrentUserCanSearch( boolean international ) {
        return checkUserCanSearch( userService.findCurrentUser(), international );
    }

    @Override
    public boolean checkUserCanUpdate( User user, UserContent userContent ) {
        // only admins or rightful owner can update user content
        return userService.hasRole( user, "ROLE_ADMIN" ) ||
                userContent.getOwner().map( u -> u.equals( user ) ).orElse( false );
    }
}
