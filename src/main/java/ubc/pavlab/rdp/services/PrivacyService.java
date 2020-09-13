package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

/**
 * These methods help determining what content are authorized to see from other users given their privacy preferences.
 */
public interface PrivacyService {

    /**
     * Check if a privacy level is enabled in the configuration.
     *
     * @param privacyLevel
     * @return
     */
    boolean isPrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    boolean isGenePrivacyLevelEnabled( PrivacyLevelType privacyLevel );

    PrivacyLevelType getDefaultPrivacyLevel();

    /**
     * Check of a given user has access to a privacy-sensitive content.
     *
     * Consider using hasPermission(object, 'read') instead.
     *
     * @param user
     * @param content
     * @return
     */
    boolean checkUserCanSee( User user, UserContent content );


    /**
     * Check if a given user can search.
     *
     * Consider using hasPermission(object, 'search') instead.
     *
     * @param user
     * @param international
     * @return
     */
    boolean checkUserCanSearch( User user, boolean international );

    boolean checkCurrentUserCanSearch( boolean international );

    boolean checkUserCanUpdate( User user, UserContent targetDomainObject );
}
