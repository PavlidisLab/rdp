package ubc.pavlab.rdp.model.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by mjacobson on 28/01/18.
 */
public enum TierType {
    TIER1, TIER2, TIER3, ANY, MANUAL;

    public static final Set<TierType> MANUAL_TIERS = EnumSet.of(TIER1, TIER2);

}
