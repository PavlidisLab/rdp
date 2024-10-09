package ubc.pavlab.rdp.util;

import org.springframework.lang.Nullable;

import java.util.Optional;

public class Version implements Comparable<Version> {

    private static final int[] FACTORS = new int[]{ 99 * 99 * 99, 99 * 99, 99 };

    public static Version parseVersion( String version ) throws VersionException {
        int i = 0;
        int v = 0;
        // pre-release component cannot be proceeded by a '.' (i.e. 1.-SNAPSHOT is invalid)
        String[] versionAndPreRelease = version.split( "[^.]-", 2 );
        String[] components = versionAndPreRelease[0].split( "\\." );
        return new Version( components, versionAndPreRelease.length > 1 ? versionAndPreRelease[1] : null );
    }

    private final int[] components;
    private final String preRelease;

    /**
     * Create a new version from an array of components and an optional pre-release.
     *
     * @throws VersionException if the components contains an invalid number
     */
    public Version( String[] components, @Nullable String preRelease ) throws VersionException {
        if ( components.length > FACTORS.length ) {
            throw new VersionException( "Version must have at most " + FACTORS.length + " components." );
        }
        this.components = new int[components.length];
        for ( int i = 0; i < components.length; i++ ) {
            int ci;
            try {
                ci = Integer.parseInt( components[i] );
            } catch ( NumberFormatException e ) {
                throw new VersionException( "Version component must be a valid integer.", e );
            }
            if ( ci < 0 || ci > 99 ) {
                throw new VersionException( "Version component must be within 0 and 99." );
            }
            this.components[i] = ci;
        }
        this.preRelease = preRelease;
    }

    @Override
    public int compareTo( Version version ) {
        return getRank() - version.getRank();
    }

    public int getMajor() {
        return components[0];
    }

    public int getMinor() {
        return components.length > 1 ? components[1] : 0;
    }

    public int getPatch() {
        return components.length > 2 ? components[2] : 0;
    }

    public Optional<String> getPreRelease() {
        return Optional.ofNullable( preRelease );
    }

    private int getRank() {
        int i = 0, v = 0;
        for ( int c : components ) {
            v += FACTORS[i++] * c;
        }
        return v;
    }
}
