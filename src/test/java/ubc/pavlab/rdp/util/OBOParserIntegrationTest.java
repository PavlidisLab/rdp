package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class OBOParserIntegrationTest {

    @TestConfiguration
    static class OBOParserTestContextConfiguration {

        @Bean
        OBOParser oboParser() {
            return new OBOParser();
        }
    }

    @Autowired
    OBOParser oboParser;

    @Test
    public void parseStream_withGoTerms_thenSucceed() throws IOException {
        Map<String, OBOParser.Term> parsedTerms = oboParser.parseStream( new ClassPathResource( "cache/go.obo" ).getInputStream() );
        assertThat(parsedTerms).containsKey("GO:0000001");
        OBOParser.Term term = parsedTerms.get("GO:0000001");
        assertThat(term)
                .hasFieldOrPropertyWithValue( "id", "GO:0000001" )
                .hasFieldOrPropertyWithValue( "name", "mitochondrion inheritance" )
                .hasFieldOrPropertyWithValue( "namespace", "biological_process" )
                .hasFieldOrPropertyWithValue( "definition", "The distribution of mitochondria, including the mitochondrial genome, into daughter cells after mitosis or meiosis, mediated by interactions between mitochondria and the cytoskeleton." );
    }

    @Test
    public void parseStream_withUberonTerms_thenSucceed() throws IOException {
        Map<String, OBOParser.Term> parsedTerms = oboParser.parseStream( new ClassPathResource( "cache/uberon.obo" ).getInputStream() );
        assertThat(parsedTerms).containsKey("UBERON:0000000");
        OBOParser.Term term = parsedTerms.get("UBERON:0000000");
        assertThat(term)
                .hasFieldOrPropertyWithValue( "name", "processual entity" )
                .hasFieldOrPropertyWithValue( "definition", "An occurrent [span:Occurrent] that exists in time by occurring or happening, has temporal parts and always involves and depends on some entity." );
    }
}
