package ubc.pavlab.rdp.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TextUtilsTest {

    @Test
    public void tokenize() {
        assertThat( TextUtils.tokenize( "a terrible DISEASE with some stop words" ) )
                .containsExactly( "A", "TERRIBLE", "DISEASE", "WITH", "SOME", "STOP", "WORDS" );
    }

    @Test
    public void tokenize_whenTokenisOntologyIdentifier_thenPreserveItWhole() {
        assertThat( TextUtils.tokenize( "MONDO:0001291" ) )
                .containsExactly( "MONDO:0001291" );
    }

    @Test
    public void normalize() {
        assertThat( TextUtils.normalize( "a  terrible disEASE   with some  STOP words%!!!" ) )
                .isEqualTo( "A TERRIBLE DISEASE WITH SOME STOP WORDS" );
    }
}