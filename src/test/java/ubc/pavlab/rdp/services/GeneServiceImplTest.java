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
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.BaseTest;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 26/02/18.
 */
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {"spring.jpa.properties.hibernate.ejb.entitymanager_factory_name = test"})
public class GeneServiceImplTest extends BaseTest {

    private static Log log = LogFactory.getLog( GeneServiceImplTest.class );

    @TestConfiguration
    static class GeneServiceImplTestContextConfiguration {

        @Bean
        public ApplicationSettings applicationSettings() {
            ApplicationSettings a = new ApplicationSettings();
            ApplicationSettings.CacheSettings cacheSettings = new ApplicationSettings.CacheSettings();
            cacheSettings.setEnabled( false );
            a.setCache( cacheSettings );
            return a;
        }

        @Bean
        public GeneService geneService() {
            return new GeneServiceImpl();
        }

        @Bean
        public EhCacheCacheManager cacheManager() {
            return new EhCacheCacheManager( EhCacheManagerUtils.buildCacheManager( "test" ) );
        }

    }

    @Autowired
    private GeneService geneService;

    @Autowired
    private EhCacheCacheManager cacheManager;

    @Autowired
    private ApplicationSettings applicationSettings;

    @MockBean
    private TaxonService taxonService;

    private Taxon taxon;
    private Map<Integer, Gene> genes;

    @Before
    public void setUp() {
        geneService.clear();
        taxon = createTaxon( 1 );
        genes = new HashMap<>();
        genes.put( 1, createGene( 1, taxon ) );
        genes.put( 2, createGene( 2, taxon ) );
        genes.put( 3, createGene( 3, taxon ) );

        genes.values().forEach( g -> {
            g.setSymbol( "Gene" + g.getGeneId() + "Symbol" );
            g.setName( "Gene" + g.getGeneId() + "Name" );
            g.setAliases( "Gene" + g.getGeneId() + "Alias" );
        } );

        geneService.addAll( genes.values() );
    }

    @Test
    public void load_whenValidId_thenReturnGene() {
        Gene found = geneService.load( 1 );
        assertThat( found ).isEqualTo( genes.get( 1 ) );
    }

    @Test
    public void load_whenInvalidId_thenReturnNull() {
        Gene found = geneService.load( -1 );
        assertThat( found ).isNull();
    }

    @Test
    public void load_whenMultipleValidIds_thenReturnGenes() {
        Collection<Gene> found = geneService.load( Sets.newSet( 1, 3 ) );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ) );
    }

    @Test
    public void load_whenMultipleValidOrInvalidIds_thenReturnValidGenesAndNulls() {
        Collection<Gene> found = geneService.load( Sets.newSet( 1, 3, 5, 7 ) );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ), null, null );
    }

    @Test
    public void findBySymbolAndTaxon_whenValidSymbolAndTaxon_thenReturnGene() {
        Gene found = geneService.findBySymbolAndTaxon( "Gene1Symbol", taxon );
        assertThat( found ).isEqualTo( genes.get( 1 ) );
    }

    @Test
    public void findBySymbolAndTaxon_whenInvalidSymbol_thenReturnNull() {
        Gene found = geneService.findBySymbolAndTaxon( "GeneX", taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void findBySymbolAndTaxon_whenInvalidSymbolCaseInsensitive_thenReturnNull() {
        Gene found = geneService.findBySymbolAndTaxon( "GeNe1SyMboL", taxon );
        assertThat( found ).isEqualTo( genes.get( 1 ) );
    }

    @Test
    public void findBySymbolAndTaxon_whenValidSymbolAndInvalidTaxon_thenReturnNull() {
        Gene found = geneService.findBySymbolAndTaxon( "Gene1Symbol", createTaxon( 2 ) );
        assertThat( found ).isNull();
    }

    @Test
    public void findBySymbolInAndTaxon_whenValidSymbolAndTaxon_thenReturnGene() {
        Collection<Gene> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "Gene1Symbol" ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ) );
    }

    @Test
    public void findBySymbolInAndTaxon_whenMultipleValidSymbolAndTaxon_thenReturnGene() {
        Collection<Gene> found = geneService.findBySymbolInAndTaxon( Sets.newSet( "Gene1Symbol", "Gene3Symbol" ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ) );
    }

    @Test
    public void findBySymbolInAndTaxon_whenInvalidSymbol_thenReturnEmpty() {
        Collection<Gene> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "GeneX" ), taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void findBySymbolInAndTaxon_whenInvalidSymbolCaseInsensitive_thenReturnEmpty() {
        Collection<Gene> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "GeNe1SYmbol" ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ) );
    }

    @Test
    public void findBySymbolInAndTaxon_whenValidSymbolAndInvalidTaxon_thenReturnEmpty() {
        Collection<Gene> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "Gene1Symbol" ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void autocomplete_whenExactSymbol_thenReturnCorrectMatch() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene1Symbol", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<Gene> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.EXACT_SYMBOL );

    }

    @Test
    public void autocomplete_whenSimilarSymbol_thenReturnCorrectMatch() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene2", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<Gene> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 2 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_SYMBOL );
    }

    @Test
    public void autocomplete_whenSimilarName_thenReturnCorrectMatch() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene3N", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<Gene> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 3 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_NAME );
    }

    @Test
    public void autocomplete_whenSimilarAlias_thenReturnCorrectMatch() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene1A", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<Gene> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_ALIAS );
    }

    @Test
    public void autocomplete_whenMultipleMatches_thenReturnAll() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene", taxon, 10 );

        assertThat( matches ).hasSize( 3 );
        assertThat( matches.stream().map( SearchResult::getMatch ).collect( Collectors.toSet() ) ).containsExactlyElementsOf( genes.values() );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toSet() ) ).containsExactly( GeneMatchType.SIMILAR_SYMBOL );
    }

    @Test
    public void autocomplete_whenMultipleMatchTypes_thenReturnCorrectOrder() {

        Map<Integer, Gene> genes = new HashMap<>();
        genes.put( 10, createGene( 10, taxon ) );
        genes.put( 11, createGene( 11, taxon ) );
        genes.put( 12, createGene( 12, taxon ) );
        genes.put( 13, createGene( 13, taxon ) );

        genes.get( 10 ).setSymbol( "Match" );
        genes.get( 11 ).setSymbol( "MatchSimilar" );
        genes.get( 12 ).setName( "Match" );
        genes.get( 13 ).setAliases( "Match" );

        geneService.addAll( genes.values() );

        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Match", taxon, 10 );

        assertThat( matches ).hasSize( 4 );
        assertThat( matches.stream().map( SearchResult::getMatch ).collect( Collectors.toList() ) )
                .containsExactly( genes.get( 10 ), genes.get( 11 ), genes.get( 12 ), genes.get( 13 ) );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toList() ) )
                .containsExactly( GeneMatchType.EXACT_SYMBOL, GeneMatchType.SIMILAR_SYMBOL, GeneMatchType.SIMILAR_NAME, GeneMatchType.SIMILAR_ALIAS );
    }

    @Test
    public void autocomplete_whenMultipleMatchesAndResultsLimited_thenReturnSome() {
        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene", taxon, 2 );

        assertThat( matches ).hasSize( 2 );
    }

    @Test
    public void autocomplete_whenMultipleMatchesAndResultsUnlimited_thenReturnAll() {

        geneService.addAll( IntStream.range( 10, 100 ).boxed().map(
                nbr -> {
                    Gene g = createGene( nbr, taxon );
                    g.setSymbol( "Gene" + nbr );
                    return g;
                }
        ).collect( Collectors.toSet() ) );

        Collection<SearchResult<Gene>> matches = geneService.autocomplete( "Gene", taxon, -1 );

        assertThat( matches ).hasSize( 93 );
    }

    @Test
    public void deserializeGenes_whenMultipleValidOrInvalidIds_thenReturnValidGenes() {
        Map<Integer, TierType> geneTierMap = new HashMap<>();
        geneTierMap.put( 1, TierType.TIER1 );
        geneTierMap.put( 2, TierType.TIER2 );
        geneTierMap.put( 3, TierType.TIER3 );
        geneTierMap.put( 4, TierType.TIER3 );
        geneTierMap.put( 5, TierType.TIER3 );
        Map<Gene, TierType> found = geneService.deserializeGenesTiers( geneTierMap );
        assertThat( found.keySet() ).containsExactly( genes.get( 1 ), genes.get( 2 ), genes.get( 3 ) );
        assertThat( found.values() ).containsExactly( TierType.TIER1, TierType.TIER2, TierType.TIER3 );
    }

}
