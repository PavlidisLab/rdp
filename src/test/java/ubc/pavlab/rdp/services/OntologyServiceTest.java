package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.ParseException;
import ubc.pavlab.rdp.util.SearchResult;

import javax.persistence.EntityManager;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@CommonsLog
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

        @Bean
        public OntologyTestSetupService ontologyTestSetupService() {
            return new OntologyTestSetupService();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private OntologyStubService ontologyStubService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OntologyTestSetupService ontologySetupService;

    @MockBean(name = "messageSourceWithoutOntology")
    private MessageSource messageSource;

    @MockBean
    private BuildProperties buildProperties;

    /**
     * This is necessary to make ontology setup transactional.
     */
    @Service
    public static class OntologyTestSetupService {

        @Autowired
        private OntologyRepository ontologyRepository;

        @Autowired
        private OntologyService ontologyService;

        @Transactional
        public Ontology setupOntology( String ont, boolean activateTerms ) throws IOException, OntologyNameAlreadyUsedException, ParseException {
            if ( ontologyRepository.existsByName( ont ) ) {
                log.info( String.format( "%s ontology already setup, skipping...", ont ) );
                return ontologyRepository.findByName( ont );
            }
            Resource resource = new ClassPathResource( "cache/" + ont + ".obo" );
            try ( Reader reader = new InputStreamReader( resource.getInputStream() ) ) {
                Ontology ontology = ontologyService.createFromObo( reader );
                ontology.setOntologyUrl( resource.getURL() );
                ontologyService.activate( ontology, activateTerms );
                return ontology;
            }
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
        term = ontologyService.findTermByTermIdAndOntology( "UBERON:0000044", ontology );
        assertThat( term ).isNotNull();
        assertThat( term.getAltTermIds() )
                .containsExactly( "UBERON:0026602" );
        OntologyTermInfo termWithMixedCaseSynonym = ontologyService.findTermByTermIdAndOntology( "UBERON:8000005", ontology );
        assertThat( termWithMixedCaseSynonym ).isNotNull();
        assertThat( termWithMixedCaseSynonym.getSynonyms() )
                .contains( "nerve fiber layer of Henle".toLowerCase() );
    }

    @Test
    public void createFromObo_whenOntologyWithSameNameAlreadyExists_thenRaiseException() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        // two ontologies with the same name cannot exist
        ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        assertThatThrownBy( () -> ontologyService.createFromObo( new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) ) )
                .isInstanceOf( OntologyNameAlreadyUsedException.class );
    }

    @Test
    public void updateFromObo() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologyService.create( Ontology.builder( "uberon" ).build() );
        entityManager.refresh( ontology );
        ontologyService.updateFromObo( ontology, new InputStreamReader( new ClassPathResource( "cache/uberon.obo" ).getInputStream() ) );
        assertThat( ontology.getTerms() ).hasSize( 14938 );
    }

    @Test
    public void autocomplete() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologySetupService.setupOntology( "uberon", true );
        entityManager.refresh( ontology );
        assertThat( ontology.isActive() ).isTrue();
        assertThat( ontology.getTerms() ).hasSize( 14938 );
        Collection<SearchResult<OntologyTermInfo>> results = ontologyService.autocompleteTerms( "bicep", 1000, Locale.getDefault() );
        assertThat( results ).extracting( "match" )
                .extracting( "termId" )
                // the topological order is not unique :(
                .containsExactlyInAnyOrder( "UBERON:0001507", "UBERON:0001374", "UBERON:0008188", "UBERON:0007168",
                        "UBERON:0007169", "UBERON:0004502", "UBERON:0018305", "UBERON:0011368", "UBERON:0011366", "UBERON:0001379",
                        "UBERON:0001506", "UBERON:0001505", "UBERON:0011110", "UBERON:0009991", "UBERON:0001414", "UBERON:0001106",
                        "UBERON:0001398", "UBERON:4200183", "UBERON:0010760" );

        assertThat( ontologyService.autocompleteTerms( "UBERON:0001507", 10, Locale.getDefault() ) ).hasSize( 1 );
        verify( messageSource, VerificationModeFactory.atLeastOnce() ).getMessage(
                argThat( am -> "rdp.ontologies.uberon.terms.biceps brachii.title".equals( ( (DefaultMessageSourceResolvable) am ).getCode() ) ),
                eq( Locale.getDefault() ) );
    }

    @Test
    public void autocomplete_whenPerformanceIsExpected_thenDeliver() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        ontologySetupService.setupOntology( "uberon", true );

        String[] queries = { "bicep", "brain", "heart", "organ", "sciatic nerve" };

        StopWatch timer;
        Collection<SearchResult<OntologyTermInfo>> results;
        for ( String query : queries ) {
            // clear cache
            entityManager.clear();

            timer = StopWatch.createStarted();
            results = ontologyService.autocompleteTerms( query, 20, Locale.getDefault() );
            timer.stop();

            assertThat( results ).isNotEmpty();
            assertThat( timer.getTime() ).isLessThan( 1500 );

            // cached results (subTerms, etc.)
            timer = StopWatch.createStarted();
            Collection<SearchResult<OntologyTermInfo>> cachedResults = ontologyService.autocompleteTerms( query, 20, Locale.getDefault() );
            timer.stop();

            assertThat( cachedResults ).containsExactlyElementsOf( results );
            assertThat( timer.getTime() ).isLessThan( 1000 );
        }
    }

    @Test
    public void autocomplete_whenMultipleTermsAreUsed_thenNarrowDownTheSearchAccordingly() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        ontologySetupService.setupOntology( "uberon", true );
        assertThat( ontologyService.autocompleteTerms( "nerve", 1000, Locale.getDefault() ) )
                .hasSize( 332 );
        assertThat( ontologyService.autocompleteTerms( "sciatic", 1000, Locale.getDefault() ) )
                .hasSize( 11 );
        assertThat( ontologyService.autocompleteTerms( "sciatic nerve", 1000, Locale.getDefault() ) )
                .hasSize( 6 );
    }

    @Test
    public void autocomplete_whenMaxResultIsLow_thenRespectTheLimit() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologySetupService.setupOntology( "uberon", true );
        entityManager.refresh( ontology );
        assertThat( ontologyService.autocompleteTerms( "bicep", 100, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 100 );
        assertThat( ontologyService.autocompleteTerms( "bicep", 10, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 10 );
        assertThat( ontologyService.autocompleteTerms( "bicep", 1, Locale.getDefault() ) )
                .size()
                .isLessThanOrEqualTo( 1 );
    }

    @Test
    public void autocomplete_whenNoMatch_thenReturnEmpty() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        Ontology ontology = ontologySetupService.setupOntology( "uberon", true );
        entityManager.refresh( ontology );
        assertThat( ontology.isActive() ).isTrue();
        assertThat( ontology.getTerms() ).hasSize( 14938 );
        Collection<SearchResult<OntologyTermInfo>> results = ontologyService.autocompleteTerms( "asdlkjasiq", 1000, Locale.getDefault() );
        assertThat( results ).hasSize( 0 );
    }

    @Test
    public void updateOntologies() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        ontologySetupService.setupOntology( "uberon", false );
        entityManager.flush();
        entityManager.clear();
        ontologyService.updateOntologies();
    }

    @Test
    public void writeObo_whenFedBackToOboParser_thenProduceExactlyTheSameStructure() throws IOException, ParseException, OntologyNameAlreadyUsedException {
        when( messageSource.getMessage( any(), any() ) ).thenAnswer( a -> {
            MessageSourceResolvable resolvable = a.getArgument( 0, MessageSourceResolvable.class );
            if ( resolvable.getDefaultMessage() != null ) {
                return resolvable.getDefaultMessage();
            } else {
                throw new NoSuchMessageException( "" );
            }
        } );
        Ontology ontology = ontologySetupService.setupOntology( "mondo", false );
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
        Ontology mondo = ontologySetupService.setupOntology( "mondo", true );
        OntologyTermInfo term = ontologyService.findTermByTermIdAndOntology( "MONDO:0000001", mondo );
        ontologyService.activateTerm( term );
        entityManager.refresh( term );
        assertThat( term.isActive() ).isTrue();
    }

    /**
     * FIXME: This test is rather slow with H2, but very fast on MySQL due to the large "in :termIds" expression used to activate terms.
     */
    @Test
    public void activateTermSubtree() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        Ontology mondo = ontologySetupService.setupOntology( "mondo", false );
        OntologyTermInfo term = ontologyService.findTermByTermIdAndOntology( "MONDO:0000001", mondo );
        int activatedTerms = ontologyService.activateTermSubtree( term );
        assertThat( activatedTerms ).isEqualTo( 22027 );
    }

    @Test
    public void inferTermIds() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        Ontology uberon = ontologySetupService.setupOntology( "uberon", true );
        OntologyTermInfo brainTerm = ontologyService.findTermByTermIdAndOntologyName( "UBERON:0000955", "uberon" );
        Set<OntologyTermInfo> inferredTerms = ontologyService.inferTermIds( Collections.singleton( brainTerm ) ).stream().map( ontologyService::findTermById ).collect( Collectors.toSet() );
        assertThat( inferredTerms )
                .extracting( "termId" )
                .containsExactlyInAnyOrder( "UBERON:0008823", "UBERON:6003624", "UBERON:6001920", "UBERON:6001060", "UBERON:0000955" );
    }

    @Test
    public void inferTermIds_whenTopTermIsUsed() throws OntologyNameAlreadyUsedException, IOException, ParseException {
        Ontology uberon = ontologySetupService.setupOntology( "uberon", true );
        StopWatch stopWatch = StopWatch.createStarted();
        OntologyTermInfo brainTerm = ontologyService.findTermByTermIdAndOntology( "UBERON:0001062", uberon );
        stopWatch.stop();
        Set<Integer> inferredTerms = ontologyService.inferTermIds( Collections.singleton( brainTerm ) );
        assertThat( inferredTerms ).hasSize( 13848 );
        assertThat( stopWatch.getTime( TimeUnit.MILLISECONDS ) ).isLessThan( 500 );
    }

    @Test
    public void move() {
        for ( int i = 0; i < 5; i++ ) {
            entityManager.persist( Ontology.builder( "ont" + i )
                    .ordering( i + 1 )
                    .active( true )
                    .build() );
        }

        assertThat( ontologyService.findAllOntologies() )
                .extracting( "name" )
                .containsExactly( "ont0", "ont1", "ont2", "ont3", "ont4" );

        Ontology ontology = ontologyService.findByName( "ont" + 3 );
        assertThat( ontology ).hasFieldOrPropertyWithValue( "ordering", 4 );

        ontologyService.move( ontology, OntologyService.Direction.UP );

        assertThat( ontologyService.findAllOntologies() )
                .extracting( "name" )
                .containsExactly( "ont0", "ont1", "ont3", "ont2", "ont4" );
    }

    @Test
    public void move_whenOntologiesDontHaveSpecifiedOrder() {
        for ( int i = 0; i < 5; i++ ) {
            entityManager.persist( Ontology.builder( "ont" + i )
                    .active( true )
                    .build() );
        }

        assertThat( ontologyService.findAllOntologies() )
                .extracting( "name" )
                .containsExactly( "ont0", "ont1", "ont2", "ont3", "ont4" );

        Ontology ontology = ontologyService.findByName( "ont" + 3 );
        assertThat( ontology ).hasFieldOrPropertyWithValue( "ordering", null );

        ontologyService.move( ontology, OntologyService.Direction.UP );

        assertThat( ontologyService.findAllOntologies() )
                .extracting( "name" )
                .containsExactly( "ont0", "ont1", "ont3", "ont2", "ont4" );
    }
}