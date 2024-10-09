package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.settings.ApplicationSettings;

/**
 * Logic regarding privacy
 */
@Service("privacyService")
@CommonsLog
public class PrivacyServiceImpl implements PrivacyService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPrivacyLevelEnabled( PrivacyLevelType privacyLevel ) {
        return applicationSettings.getPrivacy().getEnabledLevels().contains( privacyLevel ) &&
                privacyLevel.ordinal() >= applicationSettings.getPrivacy().getMinLevel();
    }

    @Override
    public boolean isGenePrivacyLevelEnabled( PrivacyLevelType privacyLevel ) {
        return isPrivacyLevelEnabled( privacyLevel ) &&
                applicationSettings.getPrivacy().getEnabledGeneLevels().contains( privacyLevel );
    }

    @Override
    public PrivacyLevelType getDefaultPrivacyLevel() {
        return PrivacyLevelType.values()[applicationSettings.getPrivacy().getDefaultLevel()];
    }
}
