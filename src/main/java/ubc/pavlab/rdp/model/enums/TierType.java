package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * Created by mjacobson on 28/01/18.
 */
@AllArgsConstructor
@Getter
public enum TierType {
    TIER1( "Tier 1" ),
    TIER2( "Tier 2" ),
    TIER3( "Tier 3" );

    private final String label;

    /**
     * This is the subset of manually assigned tiers which correspond to tier 1 and 2.
     */
    public static final Set<TierType> MANUAL = EnumSet.of( TIER1, TIER2 );

    /**
     * Set of all tiers.
     */
    public static final Set<TierType> ANY = EnumSet.allOf( TierType.class );
}
