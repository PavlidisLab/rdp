package ubc.pavlab.rdp.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionUtilsTest {

    @Test
    public void satisfiesVersion_thenMatchExpectations() {
        assertThat( VersionUtils.satisfiesVersion( "1.0.0", "1.0.0" ) ).isTrue();
        assertThat( VersionUtils.satisfiesVersion( "1.0.0-SNAPSHOT", "1.0.0" ) ).isTrue();
        assertThat( VersionUtils.satisfiesVersion( "1.0.1", "1.0.0" ) ).isTrue();
        assertThat( VersionUtils.satisfiesVersion( "0.9", "1.0.0" ) ).isFalse();
        assertThat( VersionUtils.satisfiesVersion( "0.1.0", "0.0.1" ) ).isTrue();
        assertThat( VersionUtils.satisfiesVersion( "0.99", "0.9" ) ).isTrue();
    }

    @Test(expected = VersionException.class)
    public void satisfiesVersion_whenVersionIsOver99_thenRaiseRuntimeException() {
        VersionUtils.satisfiesVersion( "1.100", "1.0.0" );
    }

    @Test(expected = VersionException.class)
    public void satisfiesVersion_whenVersionIsUnder0_thenRaiseRuntimeException() {
        VersionUtils.satisfiesVersion( "1.-1", "1.0.0" );
    }

    @Test(expected = IllegalArgumentException.class)
    public void satisfiesVersion_whenVersionIsInvalid_thenRaiseIllegalArgumentException() {
        VersionUtils.satisfiesVersion( "v0", "1.4.0" );
    }
}
