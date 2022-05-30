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
}
