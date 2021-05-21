package ubc.pavlab.rdp.services;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.repositories.GeneOntologyTermInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.SearchResult;
import ubc.pavlab.rdp.util.TestUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
public class GOServiceImplTest {

    @TestConfiguration
    static class GOServiceImplTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings a = new ApplicationSettings();
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setEnabled( false );
            cacheSettings.setTermFile( new ClassPathResource( "cache/go.obo" ) );
            cacheSettings.setAnnotationFile( new ClassPathResource( "cache/gene2go.gz" ) );
            a.setCache( cacheSettings );
            return a;
        }

        @Bean
        public GOService goService() {
            return new GOServiceImpl();
        }

        @Bean
        public Gene2GoParser gene2GoParser() {
            return new Gene2GoParser();
        }

        @Bean
        public OBOParser oboParser() {
            return new OBOParser();
        }

        @Bean
        public GeneOntologyTermInfoRepository goRepository() {
            return new GeneOntologyTermInfoRepository();
        }
    }

    @Autowired
    private GOService goService;

    private Taxon taxon;
    private Map<Integer, GeneInfo> genes;
    private Map<Integer, GeneOntologyTermInfo> terms;

    @Before
    public void setUp() {
        // Need to create a small yet representative GO hierarchy with associated genes...

        //      ___T0___
        //     /        \
        //    T1[G1]     T4[G2]
        //   /    \       \
        //  T2[G2] T3[G1] T5[G1]

        taxon = createTaxon( 1 );

        genes = new HashMap<>();

        GeneInfo g1 = createGene( 1, taxon );
        GeneInfo g2 = createGene( 2, taxon );

        genes.put( g1.getGeneId(), g1 );
        genes.put( g2.getGeneId(), g2 );

        GeneOntologyTermInfo t0 = initializeTerm( createTerm( toGOId( 0 ) ) );
        GeneOntologyTermInfo t1 = initializeTerm( createTermWithGenes( toGOId( 1 ), g1 ) );
        GeneOntologyTermInfo t2 = initializeTerm( createTermWithGenes( toGOId( 2 ), g2 ) );
        GeneOntologyTermInfo t3 = initializeTerm( createTermWithGenes( toGOId( 3 ), g1 ) );
        GeneOntologyTermInfo t4 = initializeTerm( createTermWithGenes( toGOId( 4 ), g2 ) );
        GeneOntologyTermInfo t5 = initializeTerm( createTermWithGenes( toGOId( 5 ), g1 ) );

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

        goService.save( terms.values() );
    }

    @After
    public void tearDown() {
        goService.deleteAll();
    }

    private GeneOntologyTermInfo initializeTerm( GeneOntologyTermInfo term ) {
        term.setName( "Term_" + term.getGoId() + "_Name" );
        term.setDefinition( "Term_" + term.getGoId() + "_Definition" );
        return term;
    }

    @Test(expected = NullPointerException.class)
    public void termFrequencyMap_whenGenesNull_thenThroNullPointerException() {
        Map<GeneOntologyTermInfo, Long> fmap = goService.termFrequencyMap( null );
    }

    @Test
    public void termFrequencyMap_whenGenesEmpty_thenReturnEmpty() {
        Map<GeneOntologyTermInfo, Long> fmap = goService.termFrequencyMap( Collections.EMPTY_SET );
        assertThat( fmap ).isEmpty();
    }

    @Test
    public void termFrequencyMap_whenValidGenes_thenReturnFrequencyMap() {
        Map<GeneOntologyTermInfo, Long> fmap = goService.termFrequencyMap( genes.values() );

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
        Map<GeneOntologyTermInfo, Long> fmap = goService.termFrequencyMap( genes.values() );

        assertThat( fmap.get( terms.get( 1 ) ) ).isEqualTo( 2L );
    }

    @Test
    public void search_whenExactID_thenReturnCorrectMatch() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( toGOId( 0 ), taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneOntologyTermInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 0 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.EXACT_ID );
    }

    @Test
    public void search_whenNameContains_thenReturnCorrectMatch() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "1_Name", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneOntologyTermInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.NAME_CONTAINS );
    }

    @Test
    public void search_whenDefinitionContains_thenReturnCorrectMatch() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "3_Definition", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneOntologyTermInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 3 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.DEFINITION_CONTAINS );
    }

    @Test
    public void search_whenNameContainsPart_thenReturnCorrectMatch() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "xyz 4_Name", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneOntologyTermInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 4 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.NAME_CONTAINS_PART );
    }

    @Test
    public void search_whenDefinitionContainsPart_thenReturnCorrectMatch() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "xyz 5_Definition", taxon, -1 );
        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneOntologyTermInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( terms.get( 5 ) );
        assertThat( match.getMatchType() ).isEqualTo( TermMatchType.DEFINITION_CONTAINS_PART );
    }

    @Test
    public void search_whenMultipleMatches_thenReturnAll() {
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "_Name", taxon, -1 );
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

        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "Match This", taxon, -1 );

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
        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "Term", taxon, 2 );
        assertThat( matches ).hasSize( 2 );
    }

    @Test
    public void search_whenMultipleMatchesAndResultsUnlimited_thenReturnAll() {
        goService.deleteAll();
        goService.saveAlias( IntStream.range( 10, 100 ).boxed()
                .map( TestUtils::toGOId )
                .collect( Collectors.toMap( identity(), nbr -> initializeTerm( createTerm( nbr ) ) ) ) );

        List<SearchResult<GeneOntologyTermInfo>> matches = goService.search( "Term", taxon, -1 );

        assertThat( matches ).hasSize( 90 );
    }

    @Test
    public void getChildren_whenNull_thenReturnNull() {
        Collection<GeneOntologyTermInfo> found = goService.getChildren( null );
        assertThat( found ).isNull();
    }

    @Test
    public void getChildren_whenDefault_thenReturnAllChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getChildren( terms.get( 1 ) );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 2 ), terms.get( 3 ) );
    }

    @Test
    public void getChildren_whenIncludePartOf_thenReturnAllChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getChildren( terms.get( 1 ), true );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 2 ), terms.get( 3 ) );
    }

    @Test
    public void getChildren_whenNotIncludePartOf_thenReturnIsAChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getChildren( terms.get( 1 ), false );
        assertThat( found ).containsExactly( terms.get( 2 ) );
    }

    @Test
    public void getDescendants_whenNull_thenReturnNull() {
        Collection<GeneOntologyTermInfo> found = goService.getDescendants( null );
        assertThat( found ).isNull();
    }

    @Test
    public void getDescendants_whenDefault_thenReturnAllChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getDescendants( terms.get( 0 ) );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 3 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getDescendants_whenIncludePartOf_thenReturnAllChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getDescendants( terms.get( 0 ), true );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 3 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getDescendants_whenNotIncludePartOf_thenReturnIsAChildren() {
        Collection<GeneOntologyTermInfo> found = goService.getDescendants( terms.get( 0 ), false );
        assertThat( found ).containsExactlyInAnyOrder( terms.get( 1 ), terms.get( 2 ), terms.get( 4 ), terms.get( 5 ) );
    }

    @Test
    public void getGenes_whenValidStringAndTaxon_thenReturnGenes() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ).getGoId(), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );

    }

    @Test
    public void getGenes_whenInvalidString_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( "xyz", taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenValidStringAndInvalidTaxon_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ).getGoId(), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullString_thenReturnNull() {
        String goId = null;
        Collection<GeneInfo> found = goService.getGenesInTaxon( goId, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidStringAndNullTaxon_thenReturnNull() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ).getGoId(), null );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidTerm_thenReturnGenes() {
        Collection<GeneInfo> found = goService.getGenes( terms.get( 0 ) );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenValidTermHasNoDescendantGenes_thenReturnDirectGenes() {
        Collection<GeneInfo> found = goService.getGenes( terms.get( 2 ) );
        assertThat( found ).containsExactly( genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenInvalidTerm_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenes( createTerm( toGOId( 999 ) ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTerm_thenReturnNull() {
        GeneOntologyTermInfo term = null;
        Collection<GeneInfo> found = goService.getGenes( term );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenValidTermAndTaxon_thenReturnGenes() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenInvalidTermAndValidTaxon_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( createTerm( toGOId( 999 ) ), taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenInvalidTaxon_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTermAndTaxon_thenReturnNull() {
        GeneOntologyTermInfo term = null;
        Collection<GeneInfo> found = goService.getGenesInTaxon( term, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenNullTaxon_thenReturnNull() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms.get( 0 ), null );
        assertThat( found ).isNull();
    }

    //

    @Test
    public void getGenes_whenMultipleTermsAndTaxon_thenReturnGenes() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );

        found = goService.getGenesInTaxon( Sets.newSet( terms.get( 3 ), terms.get( 5 ) ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ) );
    }

    @Test
    public void getGenes_whenMultipleTermsWithSomeInvalidAndValidTaxon_thenReturnGenes() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( Sets.newSet( terms.get( 1 ), createTerm( toGOId( 999 ) ) ), taxon );
        assertThat( found ).containsExactlyInAnyOrder( genes.get( 1 ), genes.get( 2 ) );
    }

    @Test
    public void getGenes_whenMultipleTermsAndInvalidTaxon_thenReturnEmpty() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void getGenes_whenNullTermsAndTaxon_thenReturnNull() {
        Collection<GeneOntologyTermInfo> terms = null;
        Collection<GeneInfo> found = goService.getGenesInTaxon( terms, taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void getGenes_whenMultipleTermsAndNullTaxon_thenReturnNull() {
        Collection<GeneInfo> found = goService.getGenesInTaxon( Sets.newSet( terms.get( 1 ), terms.get( 4 ) ), null );
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

    @Test
    public void updateGoTerms() {
        goService.updateGoTerms();
        assertThat( goService.getTerm( "GO:0000001" ) ).isNotNull();
    }
}