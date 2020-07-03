package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.enums.TierType;

import java.util.Set;

public interface TierService {

    boolean isTierEnabled( TierType tier);

    Set<TierType> getEnabledTiers();
}
