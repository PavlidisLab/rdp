package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.ParseException;
import ubc.pavlab.rdp.util.SearchResult;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@CommonsLog
public class OntologyServiceTest {

    @TestConfiguration
    public static class OntologyServiceTestContextConfiguration {

        @Bean
        public OntologyService ontologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository, MessageSource messageSource ) {
            return new OntologyService( ontologyRepository, ontologyTermInfoRepository, messageSource );
        }

        @Bean
        public OntologyStubService ontologyStubService( OntologyRepository ontologyRepository, OntologyService ontologyService ) {
            return new OntologyStubService( ontologyRepository, ontologyService );
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private OntologyStubService ontologyStubService;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void findAllTerms() {
        ontologyStubService.createStubs();
        List<OntologyTermInfo> terms = ontologyService.findAllTerms().collect( Collectors.toList() );
        assertThat( terms ).hasSize( 33 );
        for ( OntologyTermInfo term : terms ) {
            assertThat( term.getOntology() ).isNotNull();
        }
    }

    @Test
    public void createFromObo() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        assertThat( ontology.getName() ).isEqualTo( "uberon" );
        assertThat( ontology.isActive() ).isFalse();
        assertThat( ontology.getTerms() ).isNotEmpty();
        OntologyTermInfo term = ontology.getTerms().stream()
                .filter( t -> t.getTermId().equals( "UBERON:0000002" ) )
                .findFirst()
                .orElse( null );
        assertThat( term ).isNotNull();
        assertThat( term.getSynonyms() )
                .hasSize( 9 )
                .contains( "canalis cervicis uteri" );
        term = ontologyService.findByTermIdAndOntology( "UBERON:0000044", ontology );
        assertThat( term ).isNotNull();
        assertThat( term.getAltTermIds() )
                .containsExactly( "UBERON:0026602" );
    }

    @Test
    public void createFromObo_whenOntologyWithSameNameAlreadyExists_thenRaiseException() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        // two ontologies with the same name cannot exist
        ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        assertThatThrownBy( () -> ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) ) )
                .isInstanceOf( OntologyNameAlreadyUsedException.class );
    }

    @Test
    public void updateFromObo() throws IOException, ParseException {
        Ontology ontology = ontologyService.save( Ontology.builder( "uberon" ).build() );
        entityManager.refresh( ontology );
        ontologyService.updateFromObo( ontology, new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        assertThat( ontology.getTerms() ).hasSize( 13000 );
    }

    @Test
    public void autocomplete() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologyStubService.setupOntology( "uberon" );
        entityManager.refresh( ontology );
        assertThat( ontology.isActive() ).isTrue();
        assertThat( ontology.getTerms() ).hasSize( 14938 );
        Collection<SearchResult<OntologyTermInfo>> results = ontologyService.autocomplete( "bicep", 1000, Locale.getDefault() );
        assertThat( results ).extracting( "match" )
                .extracting( "termId" )
                // the topological order is not unique :(
                .containsExactlyInAnyOrder( "UBERON:0001507", "UBERON:0001374", "UBERON:0008188", "UBERON:0007168",
                        "UBERON:0007169", "UBERON:0004502", "UBERON:0018305", "UBERON:0011368", "UBERON:0011366", "UBERON:0001379",
                        "UBERON:0001506", "UBERON:0001505", "UBERON:0011110", "UBERON:0009991", "UBERON:0001414", "UBERON:0001106",
                        "UBERON:0001398", "UBERON:4200183", "UBERON:0010760" );

        assertThat( ontologyService.autocomplete( "UBERON:0001507", 10, Locale.getDefault() ) ).hasSize( 1 );
    }

    @Test
    public void autocomplete_whenPerformanceIsExpected_thenDeliver() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        ontologyStubService.setupOntology( "uberon" );

        String[] queries = { "bicep", "brain", "heart", "organ", "sciatic nerve" };

        StopWatch timer;
        Collection<SearchResult<OntologyTermInfo>> results;
        for ( String query : queries ) {
            // clear cache
            entityManager.clear();

            timer = StopWatch.createStarted();
            results = ontologyService.autocomplete( query, 20, Locale.getDefault() );
            timer.stop();

            assertThat( results ).isNotEmpty();
            assertThat( timer.getTime() ).isLessThan( 1500 );

            // cached results (subTerms, etc.)
            timer = StopWatch.createStarted();
            Collection<SearchResult<OntologyTermInfo>> cachedResults = ontologyService.autocomplete( query, 20, Locale.getDefault() );
            timer.stop();

            assertThat( cachedResults ).containsExactlyElementsOf( results );
            assertThat( timer.getTime() ).isLessThan( 1000 );
        }
    }

    @Test
    public void autocomplete_whenMultipleTermsAreUsed_thenNarrowDownTheSearchAccordingly() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        ontologyStubService.setupOntology( "uberon" );
        assertThat( ontologyService.autocomplete( "nerve", 1000, Locale.getDefault() ) )
                .hasSize( 332 );
        assertThat( ontologyService.autocomplete( "sciatic", 1000, Locale.getDefault() ) )
                .hasSize( 11 );
        assertThat( ontologyService.autocomplete( "sciatic nerve", 1000, Locale.getDefault() ) )
                .hasSize( 6 );
    }

    @Test
    public void autocomplete_whenMaxResultIsLow_thenRespectTheLimit() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        ontologyService.activate( ontology );
        entityManager.refresh( ontology );
        assertThat( ontologyService.autocomplete( "bicep", 100, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 100 );
        assertThat( ontologyService.autocomplete( "bicep", 10, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 10 );
        assertThat( ontologyService.autocomplete( "bicep", 1, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 1 );
    }

    @Test
    public void autocomplete_whenNoMatch_thenReturnEmpty() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        ontologyService.activate( ontology);
        entityManager.refresh( ontology );
        assertThat( ontology.isActive() ).isTrue();
        assertThat( ontology.getTerms() ).hasSize( 14938 );
        Collection<SearchResult<OntologyTermInfo>> results = ontologyService.autocomplete( "asdlkjasiq", 1000, Locale.getDefault() );
        assertThat( results ).hasSize( 0 );
    }

    @Test
    public void updateOntologies() {
        ontologyStubService.setupOntologies();
        entityManager.flush();
        entityManager.clear();
        ontologyService.updateOntologies();
    }

    @Test
    public void writeObo_whenFedBackToOboParser_thenProduceExactlyTheSameStructure() throws IOException, ParseException {
        ontologyStubService.setupOntologies();
        Ontology ontology = ontologyService.findByName( "mondo" );
        assertThat( ontology ).isNotNull();
        // the ontology relationships might not have been fully initialized (i.e. term super terms when sub terms are set)
        entityManager.refresh( ontology );
        StringWriter buf = new StringWriter();
        ontologyService.writeObo( ontology, buf );
        Map<String, OBOParser.Term> parsedTerms = new OBOParser().parse( new StringReader( buf.getBuffer().toString() ) ).getTermsByIdOrAltId();
        assertThat( ontology.getTerms().stream()
                .map( OntologyTerm::getTermId ).sorted().collect( Collectors.toList() ) )
                .containsExactlyElementsOf( parsedTerms.keySet().stream().sorted().collect( Collectors.toList() ) );
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
            assertThat( termIds.stream().sorted().collect( Collectors.toList() ) )
                    .containsExactlyElementsOf( termIds2.stream().sorted().collect( Collectors.toList() ) );

            // ensure that parent terms match
            Set<String> termIds3 = parsedTerms.get( term.getTermId() ).getInverseRelationships().stream()
                    .filter( r -> r.getTypedef().equals( OBOParser.Typedef.IS_A ) )
                    .map( OBOParser.Term.Relationship::getNode )
                    .map( OBOParser.Term::getId )
                    .collect( Collectors.toSet() );
            Set<String> termIds4 = term.getSubTerms().stream()
                    .map( OntologyTermInfo::getTermId )
                    .collect( Collectors.toSet() );
            assertThat( termIds3.stream().sorted().collect( Collectors.toList() ) )
                    .containsExactlyElementsOf( termIds4.stream().sorted().collect( Collectors.toList() ) );
        }
    }

    @Test
    public void activateTerm() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        Ontology mondo = ontologyStubService.setupOntology( "mondo" );
        OntologyTermInfo term = ontologyService.findByTermIdAndOntology( "MONDO:0000001", mondo );
        ontologyService.activateTerm( term );
        entityManager.refresh( term );
        assertThat( term.isActive() ).isTrue();
    }

    /**
     * This test is rather slow with H2, but very fast on MySQL due to the large "in :termIds" expression used to
     * activate terms.
     *
     * @throws OntologyNameAlreadyUsedException
     * @throws IOException
     * @throws ParseException
     */
    @Test
    public void activateTermSubtree() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        Ontology mondo = ontologyStubService.setupOntology( "mondo" );
        OntologyTermInfo term = ontologyService.findByTermIdAndOntology( "MONDO:0000001", mondo );
        int activatedTerms = ontologyService.activateTermSubtree( term );
        assertThat( activatedTerms ).isEqualTo( 22027 );
    }
}