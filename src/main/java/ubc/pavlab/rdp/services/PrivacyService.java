package ubc.pavlab.rdp.services;

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
}
