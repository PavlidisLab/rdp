package ubc.pavlab.rdp.model.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 28/01/18.
 */
public enum TierType {
    TIER1, TIER2, TIER3, UNKNOWN;

    public static final Set<TierType> MANUAL_TIERS = new HashSet<>( Arrays.asList(TIER1, TIER2) );
}
