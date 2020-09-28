package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service("tierService")
public class TierServiceImpl implements TierService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Override
    public boolean isTierEnabled( TierType tier ) {
        return applicationSettings.getEnabledTiers().contains( tier.toString() );
    }

    @Override
    public Set<TierType> getEnabledTiers() {
        return EnumSet.allOf( TierType.class ).stream().filter( this::isTierEnabled ).collect( Collectors.toSet() );
    }
}
