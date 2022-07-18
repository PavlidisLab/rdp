package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStreamReader;
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
    private OBOParser oboParser;

    @Test
    public void parse_withGoTerms_thenSucceed() throws IOException, ParseException {
        Map<String, OBOParser.Term> parsedTerms = oboParser.parse( new InputStreamReader( new ClassPathResource( "cache/go.obo" ).getInputStream() ),
                OBOParser.Configuration.builder()
                        .includedRelationshipTypedef( OBOParser.Typedef.PART_OF )
                        .build() ).getTermsByIdOrAltId();
        assertThat( parsedTerms ).containsKey( "GO:0000001" );
        OBOParser.Term term = parsedTerms.get( "GO:0000001" );
        assertThat( term )
                .hasFieldOrPropertyWithValue( "id", "GO:0000001" )
                .hasFieldOrPropertyWithValue( "name", "mitochondrion inheritance" )
                .hasFieldOrPropertyWithValue( "namespace", "biological_process" )
                .hasFieldOrPropertyWithValue( "definition", "The distribution of mitochondria, including the mitochondrial genome, into daughter cells after mitosis or meiosis, mediated by interactions between mitochondria and the cytoskeleton." );

        OBOParser.Term parentTerm = parsedTerms.get( "GO:0048308" );
        assertThat( term.getRelationships() ).contains( new OBOParser.Term.Relationship( parentTerm, OBOParser.Typedef.IS_A ) );
        assertThat( parentTerm.getInverseRelationships() ).contains( new OBOParser.Term.Relationship( term, OBOParser.Typedef.IS_A ) );

        // ensure that "part_of" are also included
        assertThat( parsedTerms.get( "GO:0000015" ).getRelationships() ).contains( new OBOParser.Term.Relationship( parsedTerms.get( "GO:0005829" ), OBOParser.Typedef.PART_OF ) );
        assertThat( parsedTerms.get( "GO:0005829" ).getInverseRelationships() ).contains( new OBOParser.Term.Relationship( parsedTerms.get( "GO:0000015" ), OBOParser.Typedef.PART_OF ) );
    }

    @Test
    public void parse_withUberonTerms_thenSucceed() throws IOException, ParseException {
        OBOParser.ParsingResult parsingResult = oboParser.parse( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ),
                OBOParser.Configuration.builder().build() );
        assertThat( parsingResult.getOntology().getName() ).isEqualTo( "uberon" );
        Map<String, OBOParser.Term> parsedTerms = parsingResult.getTermsByIdOrAltId();
        assertThat( parsedTerms ).containsKey( "UBERON:0000000" );
        OBOParser.Term term = parsedTerms.get( "UBERON:0000000" );
        assertThat( term )
                .hasFieldOrPropertyWithValue( "name", "processual entity" )
                .hasFieldOrPropertyWithValue( "definition", "An occurrent [span:Occurrent] that exists in time by occurring or happening, has temporal parts and always involves and depends on some entity." );
        assertThat( parsedTerms.get( "UBERON:0000002" ).getSynonyms() )
                .contains( new OBOParser.Term.Synonym( "canalis cervicis uteri", "EXACT" ) );
        assertThat( parsingResult.getTypedefs() ).extracting( "id" )
                .contains( "RO:0002473" );
    }

    @Test
    public void parse_withUberonTermsOnline_thenSucceed() throws IOException, ParseException {
        OBOParser.ParsingResult parsingResult = oboParser.parse( new InputStreamReader( new UrlResource( "https://github.com/obophenotype/uberon/releases/latest/download/uberon.obo" ).getInputStream() ),
                OBOParser.Configuration.builder().build() );
        assertThat( parsingResult.getOntology().getName() ).isEqualTo( "uberon" );
        Map<String, OBOParser.Term> parsedTerms = parsingResult.getTermsByIdOrAltId();
        assertThat( parsedTerms ).containsKey( "UBERON:0000000" );
        OBOParser.Term term = parsedTerms.get( "UBERON:0000000" );
        assertThat( term )
                .hasFieldOrPropertyWithValue( "name", "processual entity" )
                .hasFieldOrPropertyWithValue( "definition", "An occurrent [span:Occurrent] that exists in time by occurring or happening, has temporal parts and always involves and depends on some entity." );
    }

    @Test
    public void parse_withMondoTerms_thenSucceed() throws IOException, ParseException {
        OBOParser.ParsingResult parsingResult = oboParser.parse( new InputStreamReader( new ClassPathResource( "cache/mondo.obo" ).getInputStream() ),
                OBOParser.Configuration.builder().build() );
        assertThat( parsingResult.getOntology().getName() ).isEqualTo( "mondo" );
        Map<String, OBOParser.Term> parsedTerms = parsingResult.getTermsByIdOrAltId();
        assertThat( parsedTerms.get( "MONDO:0000003" ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "id", "MONDO:0000003" )
                .hasFieldOrPropertyWithValue( "name", "obsolete 17-hydroxysteroid dehydrogenase deficiency" )
                .hasFieldOrPropertyWithValue( "obsolete", true );
    }

    @Test
    public void parse_withNboTerms_ignoreExernallyDefinedTerms() throws IOException, ParseException {
        OBOParser.ParsingResult parsingResult = oboParser.parse( new InputStreamReader( new ClassPathResource( "cache/nbo-base.obo" ).getInputStream() ) );
        assertThat( parsingResult.getTermsByIdOrAltId() )
                .containsKey( "NBO:0000013" )
                .doesNotContainKey( "GO:0040011" ); // this term is externally defined, so it should not appear here
    }
}
