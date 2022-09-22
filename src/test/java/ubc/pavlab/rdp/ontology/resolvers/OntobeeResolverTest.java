package ubc.pavlab.rdp.ontology.resolvers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class OntobeeResolverTest {

    @TestConfiguration
    public static class ReactomeResolverTestContextConfiguration {

        @Bean
        public OntologyResolver ontologyResolver() {
            return new OntobeeResolver();
        }
    }

    @Autowired
    private OntologyResolver resolver;

    @Test
    public void resolveViewTermUrl() throws MalformedURLException {
        Ontology ontology = Ontology.builder( "mondo" )
                .ontologyUrl( new URL( "http://purl.obolibrary.org/obo/mondo.owl" ) )
                .build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "MONDO:0021166" ).build();
        assertThat( resolver.accepts( ontology ) ).isTrue();
        assertThat( resolver.resolveViewOntologyUrl( ontology ) )
                .isEqualTo( URI.create( "https://ontobee.org/ontology/MONDO" ) );
        assertThat( resolver.resolveViewTermUrl( term ) )
                .isEqualTo( URI.create( "https://ontobee.org/ontology/MONDO?iri=http%3A%2F%2Fpurl.obolibrary.org%2Fobo%2FMONDO_0021166" ) );
    }
}
