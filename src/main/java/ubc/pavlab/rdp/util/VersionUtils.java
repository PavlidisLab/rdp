package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;

@CommonsLog
public class VersionUtils {

    private static final int[] FACTORS = new int[]{ 99 * 99 * 99, 99 * 99, 99 };

    private static int parseVersion( String version ) throws IllegalArgumentException {
        int i = 0;
        int v = 0;
        String[] components = version.split( "\\." );
        for ( String c : components ) {
            int ci = Integer.parseInt( c );
            if ( ci < 0 || ci > 99 ) {
                throw new IllegalArgumentException( "Version component must be within 0 and 99." );
            }
            v += FACTORS[i++] * ci;
        }
        return v;
    }

    /**
     * Test if a version satsifies a required version.
     */
    public static boolean satisfiesVersion( String version, String requiredVersion ) {
        return parseVersion( version ) >= parseVersion( requiredVersion );
    }

}
