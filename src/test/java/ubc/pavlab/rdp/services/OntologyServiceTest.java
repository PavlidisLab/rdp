package ubc.pavlab.rdp.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.ParseException;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
public class OntologyServiceTest {

    @TestConfiguration
    public static class OntologyServiceTestContextConfiguration {

        @Bean
        public OntologyService ontologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository ) {
            return new OntologyService( ontologyRepository, ontologyTermInfoRepository );
        }

        @Bean
        public OntologyStubService ontologyStubService( OntologyRepository ontologyRepository ) {
            return new OntologyStubService( ontologyRepository );
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private OntologyStubService ontologyStubService;

    @Autowired
    private EntityManager entityManager;

    private List<Ontology> ontologies;

    @Before
    public void setUp() {
        ontologies = ontologyStubService.findAllOntologyStubs();
    }

    @Test
    public void findAllTerms() {
        List<OntologyTermInfo> terms = ontologyService.findAllTerms();
        assertThat( terms ).hasSize( 33 );
        for ( OntologyTermInfo term : terms ) {
            assertThat( term.getOntology() ).isNotNull();
        }
    }

    @Test
    public void updateOntologies() {
        ontologyService.updateOntologies();
    }


    @Test
    public void writeObo_whenFedBackToOboParser_thenProduceExactlyTheSameStructure() throws IOException, ParseException {
        Ontology ontology = ontologies.get( 0 );
        // the ontology relationships might not have been fully initialized (i.e. term super terms when sub terms are set)
        entityManager.refresh( ontology );
        StringWriter buf = new StringWriter();
        ontologyService.writeObo( ontology, buf );
        Map<String, OBOParser.Term> parsedTerms = new OBOParser().parse( new StringReader( buf.getBuffer().toString() ) );
        assertThat( ontology.getTerms().stream()
                .map( OntologyTerm::getTermId ).collect( Collectors.toSet() ) )
                .containsExactlyElementsOf( parsedTerms.keySet() );
        for ( OntologyTermInfo term : ontology.getTerms() ) {
            assertThat( parsedTerms.get( term.getTermId() ) )
                    .hasFieldOrPropertyWithValue( "id", term.getTermId() )
                    .hasFieldOrPropertyWithValue( "name", term.getName() )
                    .hasFieldOrPropertyWithValue( "definition", term.getDefinition() );

            // ensure that parent terms match
            Set<String> termIds = parsedTerms.get( term.getTermId() ).getRelationships().stream()
                    .filter( r -> r.getTypedef().equals( OBOParser.Typedef.IS_A ) )
                    .map( OBOParser.Term.Relationship::getNode )
                    .map( OBOParser.Term::getId )
                    .collect( Collectors.toSet() );
            Set<String> termIds2 = term.getSuperTerms().stream()
                    .map( OntologyTermInfo::getTermId )
                    .collect( Collectors.toSet() );
            assertThat( termIds ).containsExactlyElementsOf( termIds2 );

            // ensure that parent terms match
            Set<String> termIds3 = parsedTerms.get( term.getTermId() ).getInverseRelationships().stream()
                    .filter( r -> r.getTypedef().equals( OBOParser.Typedef.IS_A ) )
                    .map( OBOParser.Term.Relationship::getNode )
                    .map( OBOParser.Term::getId )
                    .collect( Collectors.toSet() );
            Set<String> termIds4 = term.getSubTerms().stream()
                    .map( OntologyTermInfo::getTermId )
                    .collect( Collectors.toSet() );
            assertThat( termIds3 ).containsExactlyElementsOf( termIds4 );
        }
    }
}