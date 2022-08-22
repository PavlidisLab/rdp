package ubc.pavlab.rdp.ontology.resolvers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.ReactomeService;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class ReactomeResolverTest {

    @TestConfiguration
    public static class ReactomeResolverTestContextConfiguration {

        @Bean
        public OntologyResolver ontologyResolver() {
            return new ReactomeResolver();
        }
    }

    @Autowired
    private OntologyResolver resolver;

    @MockBean
    private ReactomeService reactomeService;

    @Test
    public void resolveViewTermUrl() {
        Ontology reactome = Ontology.builder( "reactome" ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( reactome, "R-123" ).build();
        when( reactomeService.findPathwaysOntology() ).thenReturn( reactome );
        assertThat( resolver.accepts( reactome ) ).isTrue();
        assertThat( resolver.resolveViewOntologyUrl( reactome ) ).isEqualTo( URI.create( "https://reactome.org/PathwayBrowser/" ) );
        assertThat( resolver.resolveViewTermUrl( term ) ).isEqualTo( URI.create( "https://reactome.org/PathwayBrowser/#/R-123" ) );
        verify( reactomeService ).findPathwaysOntology();
    }
}