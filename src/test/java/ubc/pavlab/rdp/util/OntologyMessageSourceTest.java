package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.settings.SiteSettings;

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
    private SiteSettings siteSettings;

    @MockBean
    private OntologyService ontologyService;

    @Test
    public void resolveCode() {
        when( ontologyService.findDefinitionByTermNameAndOntologyName( "UBERON:000001", "uberon" ) )
                .thenReturn( "Lots of parts." );
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:000001.definition", null, Locale.getDefault() ) )
                .isEqualTo( "Lots of parts." );
        verify( ontologyService ).findDefinitionByTermNameAndOntologyName( "UBERON:000001", "uberon" );
    }

    @Test
    public void resolveCode_whenCodeContainsAViewTermUrlRequest_thenResolveAccordingly() {
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.view-term-url-pattern", new Object[]{ "chebi", "CHEBI_24431" }, Locale.getDefault() ) )
                .isEqualTo( "https://www.ebi.ac.uk/ols/ontologies/chebi/terms?iri=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FCHEBI_24431" );
    }

    @Test(expected = NoSuchMessageException.class)
    public void resolveCode_whenCodeIsMissingForDefinition_thenRaiseException() {
        messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:000001.definition", null, Locale.getDefault() );
    }

    @Test
    public void resolveCode_whenCodeIsReactome_thenUseDefaults() {
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.title", null, Locale.getDefault() ) )
                .isEqualTo( "Reactome Pathways" );
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.definition", null, Locale.getDefault() ) )
                .isEqualTo( "Reactome is an open-source, open access, manually curated and peer-reviewed pathway database." );
        assertThat( messageSource.getMessage( "rdp.ontologies.reactome.view-term-url-pattern", new Object[]{ "reactome", "R-123" }, Locale.getDefault() ) )
                .isEqualTo( "https://reactome.org/content/detail/R-123" );
    }
}