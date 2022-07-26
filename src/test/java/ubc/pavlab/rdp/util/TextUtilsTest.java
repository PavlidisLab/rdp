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
    public void tokenize_whenQueryContainsNumbers_thenIgnoreSinceTheyBreakFullTextSearch() {
        assertThat( TextUtils.tokenize( "type 1 diabetes" ) )
                .containsExactly( "TYPE", "1", "DIABETES" );
    }

    @Test
    public void tokenize_whenTokenisOntologyIdentifier_thenPreserveItWhole() {
        assertThat( TextUtils.tokenize( "MONDO:0001291" ) )
                .containsExactly( "MONDO:0001291" );
        assertThat( TextUtils.tokenize( "R-HSA-164843" ) )
                .containsExactly( "R-HSA-164843" );
        assertThat( TextUtils.tokenize( "REACT_118575" ) )
                .containsExactly( "REACT_118575" );
    }

    @Test
    public void normalize() {
        assertThat( TextUtils.normalize( "a  terrible disEASE   with some  STOP words%!!!" ) )
                .isEqualTo( "A TERRIBLE DISEASE WITH SOME STOP WORDS" );
    }
}