package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.BaseTest;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
public class GOServiceImplTest extends BaseTest {

    private static Log log = LogFactory.getLog( GOServiceImplTest.class );

    @TestConfiguration
    static class GOServiceImplTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings a = new ApplicationSettings();
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setEnabled( false );
            a.setCache( cacheSettings );
            return a;
        }

        @Bean
        public GOService goService() {
            return new GOServiceImpl();
        }

    }

    @Autowired
    private GOService goService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @MockBean
    private TaxonService taxonService;

    @MockBean
    private GeneService geneService;

    private Taxon taxon;
    private Map<Integer, Gene> genes;
    private Map<Integer, GeneOntologyTerm> terms;

    @Before
    public void setUp() {
        // Need to create a small yet representative GO hierachy with associated genes...

        //      ___T0___
        //     /        \
        //    T1[G1]     T4[G2]
        //   /    \       \
        //  T2[G2] T3[G1] T5[G1]

        taxon = createTaxon( 1 );

        genes = new HashMap<>();

        Gene g1 = createGene( 1, taxon );
        Gene g2 = createGene( 2, taxon );

        genes.put( g1.getGeneId(), g1 );
        genes.put( g2.getGeneId(), g2 );

        GeneOntologyTerm t0 = initializeTerm( createTerm( toGOId( 0 ) ) );
        GeneOntologyTerm t1 = initializeTerm( createTermWithGene( toGOId( 1 ), g1 ) );
        GeneOntologyTerm t2 = initializeTerm( createTermWithGene( toGOId( 2 ), g2 ) );
        GeneOntologyTerm t3 = initializeTerm( createTermWithGene( toGOId( 3 ), g1 ) );
        GeneOntologyTerm t4 = initializeTerm( createTermWithGene( toGOId( 4 ), g2 ) );
        GeneOntologyTerm t5 = initializeTerm( createTermWithGene( toGOId( 5 ), g1 ) );

        terms = new HashMap<>();
        terms.put( 0, t0 );
        terms.put( 1, t1 );
        terms.put( 2, t2 );
        terms.put( 3, t3 );
        terms.put( 4, t4 );
        terms.put( 5, t5 );

        t0.getChildren().add( new Relationship( t1, RelationshipType.IS_A ) );
        t0.getChildren().add( new Relationship( t4, RelationshipType.IS_A ) );
        t1.getParents().add( new Relationship( t0, RelationshipType.IS_A ) );
        t4.getParents().add( new Relationship( t0, RelationshipType.IS_A ) );

        t1.getChildren().add( new Relationship( t2, RelationshipType.IS_A ) );
        t1.getChildren().add( new Relationship( t3, RelationshipType.PART_OF ) );
        t2.getParents().add( new Relationship( t1, RelationshipType.IS_A ) );
        t3.getParents().add( new Relationship( t1, RelationshipType.PART_OF ) );

        t4.getChildren().add( new Relationship( t5, RelationshipType.IS_A ) );
        t5.getParents().add( new Relationship( t4, RelationshipType.IS_A ) );

        goService.setTerms( terms.values().stream().collect( Collectors.toMap( GeneOntologyTerm::getGoId, Function.identity() ) ) );

    }

    private GeneOntologyTerm initializeTerm( GeneOntologyTerm term ) {
        term.setName( "Term_" + term.getGoId() + "_Name" );
        term.setDefinition( "Term_" + term.getGoId() + "_Definition" );
        return term;
    }

    @Test
    public void termFrequencyMap_whenGenesNull_thenReturnNull() {
        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( null );
        assertThat( fmap ).isNull();
    }

    @Test
    public void termFrequencyMap_whenGenesEmpty_thenReturnEmpty() {
        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( Collections.EMPTY_SET );
        assertThat( fmap ).isEmpty();
    }

    @Test
    public void termFrequencyMap_whenValidGenes_thenReturnFrequencyMap() {
        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( genes.values() );

        Map<GeneOntologyTerm, Long> expected = new HashMap<>();
        expected.put( terms.get( 0 ), 2L );
        expected.put( terms.get( 1 ), 2L );
        expected.put( terms.get( 2 ), 1L );
        expected.put( terms.get( 3 ), 1L );
        expected.put( terms.get( 4 ), 2L );
        expected.put( terms.get( 5 ), 1L );

        assertThat( fmap ).isEqualTo( expected );
    }

    @Test
    public void termFrequencyMap_whenParentAndChildAnnotatedToSameGene_thenOnlyCountOne() {
        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( genes.values() );

        assertThat( fmap.get( terms.get( 1 ) ) ).isEqualTo( 2L );
    }

    @Test
    public void search_whenExactID_thenReturnCorrectMatch() {
        List<SearchResult<UserTerm>> matches = goService.search( toGOId( 0 ), taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<UserTerm> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 0 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.EXACT_ID );
    }

    @Test
    public void search_whenNameContains_thenReturnCorrectMatch() {
        List<SearchResult<UserTerm>> matches = goService.search( "1_Name", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<UserTerm> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.NAME_CONTAINS );
    }

    @Test
    public void search_whenDefinitionContains_thenReturnCorrectMatch() {
        List<SearchResult<UserTerm>> matches = goService.search( "3_Definition", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<UserTerm> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 3 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.DEFINITION_CONTAINS );
    }

    @Test
    public void search_whenNameContainsPart_thenReturnCorrectMatch() {
        List<SearchResult<UserTerm>> matches = goService.search( "xyz 4_Name", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<UserTerm> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 4 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.NAME_CONTAINS_PART );
    }

    @Test
    public void search_whenDefinitionContainsPart_thenReturnCorrectMatch() {
        List<SearchResult<UserTerm>> matches = goService.search( "xyz 5_Definition", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<UserTerm> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 5 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.DEFINITION_CONTAINS_PART );
    }

    @Test
    public void search_whenMultipleMatches_thenReturnAll() {
        List<SearchResult<UserTerm>> matches = goService.search( "_Name", taxon, -1 );
        assertThat( matches ).hasSize( 6 );
        assertThat( matches.stream().map( sr -> sr.getMatch().getGoId() ).collect( Collectors.toSet() ) )
                .containsExactlyElementsOf( terms.values().stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toSet() ) ).containsExactly( TermMatchType.NAME_CONTAINS );
    }

    @Test
    public void search_whenMultipleMatchTypes_thenReturnCorrectOrder() {
        terms.get( 0 ).setGoId( "Match This" );
        terms.get( 1 ).setName( "--Match This--" );
        terms.get( 2 ).setDefinition( "--Match This--" );
        terms.get( 3 ).setName( "Match" );
        terms.get( 4 ).setDefinition( "This" );

        List<SearchResult<UserTerm>> matches = goService.search( "Match This", taxon, -1 );

        assertThat( matches ).hasSize( 5 );
        assertThat( matches.stream().map( sr -> sr.getMatch().getGoId() ).collect( Collectors.toList() ) )
                .containsExactly(
                        terms.get( 0 ).getGoId(),
                        terms.get( 1 ).getGoId(),
                        terms.get( 2 ).getGoId(),
                        terms.get( 3 ).getGoId(),
                        terms.get( 4 ).getGoId() );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toList() ) )
                .containsExactly( TermMatchType.EXACT_ID,
                        TermMatchType.NAME_CONTAINS,
                        TermMatchType.DEFINITION_CONTAINS,
                        TermMatchType.NAME_CONTAINS_PART,
                        TermMatchType.DEFINITION_CONTAINS_PART );
    }

    @Test
    public void search_whenMultipleMatchesAndResultsLimited_thenReturnSome() {
        List<SearchResult<UserTerm>> matches = goService.search( "Term", taxon, 2 );
        assertThat( matches ).hasSize( 2 );
    }

    @Test
    public void search_whenMultipleMatchesAndResultsUnlimited_thenReturnAll() {

        goService.setTerms(
                IntStream.range( 10, 100 ).boxed().collect(
                        Collectors.toMap( this::toGOId,
                                nbr -> initializeTerm( createTerm( toGOId( nbr ) ) ) )
                )
        );

        List<SearchResult<UserTerm>> matches = goService.search( "Term", taxon, -1 );

        assertThat( matches ).hasSize( 90 );

    }

    @Test
    public void getChildren_whenNull_thenReturnNull() {
        Collection<GeneOntologyTerm> found = goService.getChildren( null );
        assertThat( found ).isNull();
    }

    @Test
    public void getChildren_whenDefault_thenReturnAllChildren() {
        Collection<GeneOntologyTerm> found = goService.getChildren( terms.get( 1 ) );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 2 ), terms.get( 3 ) );
    }

    @Test
    public void getChildren_whenIncludePartOf_thenReturnAllChildren() {
        Collection<GeneOntologyTerm> found = goService.getChildren( terms.get( 1 ), true );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 2 ), terms.get( 3 ) );
    }

    @Test
    public void getChildren_whenNotIncludePartOf_thenReturnIsAChildren() {
        Collection<GeneOntologyTerm> found = goService.getChildren( terms.get( 1 ), false );
        assertThat( found ).containsExactly( terms.get( 2 ) );
    }

    @Test
    public void getDescendants_whenNull_thenReturnNull() {
        Collection<GeneOntologyTerm> found = goService.getDescendants( null );
        assertThat( found ).isNull();
    }

    @Test
    public void getDescendants_whenDefault_thenReturnAllChildren() {
        Collection<GeneOntologyTerm> found = goService.getDescendants( terms.get( 0 ) );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 3 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getDescendants_whenIncludePartOf_thenReturnAllChildren() {
        Collection<GeneOntologyTerm> found = goService.getDescendants( terms.get( 0 ), true );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 3 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getDescendants_whenNotIncludePartOf_thenReturnIsAChildren() {
        Collection<GeneOntologyTerm> found = goService.getDescendants( terms.get( 0 ), false );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getGenes_whenValidStringAndTaxon_thenReturnGenes() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ).getGoId(), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );

    }

    @Test
    public void getGenes_whenInvalidString_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( "xyz", taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenValidStringAndInvalidTaxon_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ).getGoId(), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullString_thenReturnNull() {
        String goId = null;
        Collection<Gene> found = goService.getGenes( goId, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidStringAndNullTaxon_thenReturnNull() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ).getGoId(), null );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidTerm_thenReturnGenes() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ) );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenValidTermHasNoDescendantGenes_thenReturnDirectGenes() {
        Collection<Gene> found = goService.getGenes( terms.get( 2 ) );
        assertThat( found ).containsExactly( genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenInvalidTerm_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( createTerm( toGOId( 999 ) ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTerm_thenReturnNull() {
        GeneOntologyTerm term = null;
        Collection<Gene> found = goService.getGenes( term );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidTermAndTaxon_thenReturnGenes() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenInvalidTermAndValidTaxon_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( createTerm( toGOId( 999 ) ), taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenInvalidTaxon_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTermAndTaxon_thenReturnNull() {
        GeneOntologyTerm term = null;
        Collection<Gene> found = goService.getGenes( term, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenNullTaxon_thenReturnNull() {
        Collection<Gene> found = goService.getGenes( terms.get( 0 ), null );
        assertThat( found ).isNull();
    }

    //

    @Test
    public void getGenes_whenMultipleTermsAndTaxon_thenReturnGenes() {
        Collection<Gene> found = goService.getGenes( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );

        found = goService.getGenes( Sets.newSet( terms.get( 3 ), terms.get( 5 ) ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ) );
    }

    @Test
    public void getGenes_whenMultipleTermsWithSomeInvalidAndValidTaxon_thenReturnGenes() {
        Collection<Gene> found = goService.getGenes( Sets.newSet( terms.get( 1 ), createTerm( toGOId( 999 ) ) ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenMultipleTermsAndInvalidTaxon_thenReturnEmpty() {
        Collection<Gene> found = goService.getGenes( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTermsAndTaxon_thenReturnNull() {
        Collection<GeneOntologyTerm> terms = null;
        Collection<Gene> found = goService.getGenes( terms, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenMultipleTermsAndNullTaxon_thenReturnNull() {
        Collection<Gene> found = goService.getGenes( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), null );
        assertThat( found ).isNull();
    }

    @Test
    public void getTerm_whenValidId_thenReturnTerm() {
        GeneOntologyTerm found = goService.getTerm( terms.get( 1 ).getGoId() );
        assertThat( found ).isEqualTo( terms.get( 1 ) );
    }

    @Test
    public void getTerm_whenInvalidId_thenReturnNull() {
        GeneOntologyTerm found = goService.getTerm( "xyz" );
        assertThat( found ).isNull();
    }

    @Test
    public void getTerm_whenNullId_thenReturnNull() {
        GeneOntologyTerm found = goService.getTerm( null );
        assertThat( found ).isNull();
    }


}