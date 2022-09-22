package ubc.pavlab.rdp.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Locale;
import java.util.Optional;

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
                .thenReturn( Optional.of( "Lots of parts." ) );
        assertThat( messageSource.getMessage( "rdp.ontologies.uberon.terms.UBERON:000001.definition", null, Locale.getDefault() ) )
                .isEqualTo( "Lots of parts." );
        verify( ontologyService ).findDefinitionByTermNameAndOntologyName( "UBERON:000001", "uberon" );
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
    }

    @Test
    public void resolveCode_whenCodeIsAnOntologyDefinition() {
        when( ontologyService.findDefinitionByOntologyName( "mondo" ) ).thenReturn( "A diseases ontology." );
        assertThat( messageSource.getMessage( "rdp.ontologies.mondo.definition", null, Locale.getDefault() ) )
                .isEqualTo( "A diseases ontology." );
        verify( ontologyService ).findDefinitionByOntologyName( "mondo" );
    }

    @Test
    public void resolveCode_whenCodeIsAnOntologyDefinitionMessageSourceResolvable_thenResolveItCorrectly() {
        Ontology ontology = Ontology.builder( "mondo" ).build();
        when( ontologyService.findDefinitionByOntologyName( "mondo" ) ).thenReturn( "A disease ontology." );
        assertThat( messageSource.getMessage( ontology.getResolvableDefinition(), Locale.getDefault() ) )
                .isEqualTo( "A disease ontology." );
        verify( ontologyService ).findDefinitionByOntologyName( "mondo" );
    }

    @Test
    public void resolveCode_whenCodeIsATermDefinitionMessageSourceResolvable_thenResolveItCorrectly2() {
        Ontology ontology = Ontology.builder( "mondo" ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "MONDO:000001" )
                .name( "term-1" )
                .build();
        when( ontologyService.findDefinitionByTermNameAndOntologyName( "term-1", "mondo" ) )
                .thenReturn( Optional.of( "A terrible disease." ) );
        assertThat( messageSource.getMessage( term.getResolvableDefinition(), Locale.getDefault() ) )
                .isEqualTo( "A terrible disease." );
        verify( ontologyService ).findDefinitionByTermNameAndOntologyName( "term-1", "mondo" );
    }
}