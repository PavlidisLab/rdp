package ubc.pavlab.rdp.util;

public class VersionUtils {

    /**
     * Test if a version satisfies a required version.
     */
    public static boolean satisfiesVersion( String version, String requiredVersion ) throws VersionException {
        return Version.parseVersion( version ).compareTo( Version.parseVersion( requiredVersion ) ) >= 0;
    }
}
