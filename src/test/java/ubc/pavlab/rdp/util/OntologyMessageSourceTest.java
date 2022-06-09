package ubc.pavlab.rdp.util;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@Import(WebMvcConfig.class)
public class OntologyMessageSourceTest {

    @TestConfiguration
    public static class OntologyMessageSourceTestContextConfiguration {

        @Bean
        public OntologyMessageSource ontologyMessageSource() {
            return new OntologyMessageSource();
        }
    }

    @Autowired
    private MessageSource messageSource;

    @MockBean
    private OntologyTermInfoRepository ontologyTermInfoRepository;

    @Test
    public void resolveCode() {
        when( ontologyTermInfoRepository.findAllByActiveTrueAndNameAndOntologyName( "UBERON:000001", "uberon" ) )
                .thenReturn( Lists.newArrayList( OntologyTermInfo.builder( Ontology.builder( "uberon" ).build(), "uberon" )
                        .definition( "Lots of parts." ).build() ) );
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:000001.definition", null, Locale.getDefault() ) )
                .isEqualTo( "Lots of parts." );
        verify( ontologyTermInfoRepository ).findAllByActiveTrueAndNameAndOntologyName( "UBERON:000001", "uberon" );
    }

    @Test
    public void resolveCode_whenCodeContainsAViewTermUrlRequest_thenResolveAccordingly() {
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.view-term-url-pattern", new Object[]{ "chebi", "CHEBI_24431" }, Locale.getDefault() ) )
                .isEqualTo( "https://www.ebi.ac.uk/ols/ontologies/chebi/terms?iri=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FCHEBI_24431" );
    }

    @Test
    public void resolveCode_whenCodeIsUnknownForDefinition_thenReturnEmptyString() {
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:000001.definition", null, Locale.getDefault() ) )
                .isEqualTo( "" );
    }

    @Test
    public void resolveCode_whenCodeIsReactome_thenUseDefaults() {
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.title", null, Locale.getDefault() ) )
                .isEqualTo( "Reactome" );
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.definition", null, Locale.getDefault() ) )
                .isEqualTo( "Reactome is an open-source, open access, manually curated and peer-reviewed pathway database." );
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.view-term-url-pattern", new Object[]{ "reactome", "R-123" }, Locale.getDefault() ) )
                .isEqualTo( "https://reactome.org/content/detail/R-123" );
    }

}