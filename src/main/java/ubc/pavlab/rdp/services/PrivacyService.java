package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

/**
 * These methods help determining what content are authorized to see from other users given their privacy preferences.
 */
public interface PrivacyService {

    /**
     * Check if a privacy level is enabled in the configuration.
     */
    boolean isPrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    boolean isGenePrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    PrivacyLevelType getDefaultPrivacyLevel();

    /**
     * Check of a given user has access to a privacy-sensitive content.
     * <p>
     * Consider using hasPermission(object, 'read') instead.
     */
    boolean checkUserCanSee( User user, UserContent content );

    /**
     * Check if a given user can see another user's gene list.
     */
    boolean checkCurrentUserCanSeeGeneList( User otherUser );


    /**
     * Check if a given user can search.
     * <p>
     * Consider using hasPermission(object, 'search') instead.
     */
    boolean checkUserCanSearch( User user, boolean international );

    boolean checkCurrentUserCanSearch( boolean international );

    boolean checkUserCanUpdate( User user, UserContent targetDomainObject );
}
