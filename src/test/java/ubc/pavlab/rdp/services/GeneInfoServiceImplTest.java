package ubc.pavlab.rdp.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GeneInfoParser;
import ubc.pavlab.rdp.util.GeneOrthologsParser;
import ubc.pavlab.rdp.util.SearchResult;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static ubc.pavlab.rdp.util.TestUtils.createGene;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

/**
 * Created by mjacobson on 26/02/18.
 * <p>
 * TODO: move some of the tests in GeneInfoRepositoryTest so that the data repository can be mocked.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.ejb.entitymanager_factory_name = test" })
public class GeneInfoServiceImplTest {

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
        public GeneInfoService geneInfoService() {
            return new GeneInfoServiceImpl();
        }
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private GeneInfoRepository geneInfoRepository;

    @MockBean
    private GeneOrthologsParser geneOrthologsParser;

    @MockBean
    private GeneInfoParser geneInfoParser;

    @MockBean
    private UserService userService;

    @MockBean
    private TaxonService taxonService;

    private Taxon taxon;
    private Map<Integer, GeneInfo> genes;

    @Before
    public void setUp() {
        taxon = entityManager.persist( createTaxon( 1 ) );
        genes = new HashMap<>();
        genes.put( 1, entityManager.persist( createGene( 1, taxon ) ) );
        genes.put( 2, entityManager.persist( createGene( 2, taxon ) ) );
        genes.put( 3, entityManager.persist( createGene( 3, taxon ) ) );

        for ( Map.Entry<Integer, GeneInfo> entry : genes.entrySet() ) {
            entry.getValue().setSymbol( "Gene" + entry.getValue().getGeneId() + "Symbol" );
            entry.getValue().setName( "Gene" + entry.getValue().getGeneId() + "Name" );
            entry.getValue().setAliases( "Gene" + entry.getValue().getGeneId() + "Alias" );
            genes.put( entry.getKey(), entityManager.persist( entry.getValue() ) );
        }
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
        Collection<GeneInfo> found = geneService.load( Sets.newSet( 1, 3 ) );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ) );
    }

    @Test
    public void load_whenMultipleValidOrInvalidIds_thenReturnValidGenesAndNulls() {
        Collection<GeneInfo> found = geneService.load( Sets.newSet( 1, 3, 5, 7 ) );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ) );
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
        GeneInfo found = geneService.findBySymbolAndTaxon( "GeNe1SyMboL", taxon );
        assertThat( found ).isNull();
    }

    @Test
    public void findBySymbolAndTaxon_whenValidSymbolAndInvalidTaxon_thenReturnNull() {
        Gene found = geneService.findBySymbolAndTaxon( "Gene1Symbol", createTaxon( 2 ) );
        assertThat( found ).isNull();
    }

    @Test
    public void findBySymbolInAndTaxon_whenValidSymbolAndTaxon_thenReturnGene() {
        Collection<GeneInfo> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "Gene1Symbol" ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ) );
    }

    @Test
    public void findBySymbolInAndTaxon_whenMultipleValidSymbolAndTaxon_thenReturnGene() {
        Collection<GeneInfo> found = geneService.findBySymbolInAndTaxon( Sets.newSet( "Gene1Symbol", "Gene3Symbol" ), taxon );
        assertThat( found ).containsExactly( genes.get( 1 ), genes.get( 3 ) );
    }

    @Test
    public void findBySymbolInAndTaxon_whenInvalidSymbol_thenReturnEmpty() {
        Collection<GeneInfo> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "GeneX" ), taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void findBySymbolInAndTaxon_whenInvalidSymbolCaseInsensitive_thenReturnEmpty() {
        Collection<GeneInfo> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "GeNe1SYmbol" ), taxon );
        assertThat( found ).isEmpty();
    }

    @Test
    public void findBySymbolInAndTaxon_whenValidSymbolAndInvalidTaxon_thenReturnEmpty() {
        Collection<GeneInfo> found = geneService.findBySymbolInAndTaxon( Collections.singleton( "Gene1Symbol" ), createTaxon( 2 ) );
        assertThat( found ).isEmpty();
    }

    @Test
    public void autocomplete_whenExactSymbol_thenReturnCorrectMatch() {
        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene1Symbol", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.EXACT_SYMBOL );

    }

    @Test
    public void autocomplete_whenSimilarSymbol_thenReturnCorrectMatch() {
        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene2", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 2 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_SYMBOL );
    }

    @Test
    public void autocomplete_whenSimilarName_thenReturnCorrectMatch() {

        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene3N", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 3 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_NAME );
    }

    @Test
    public void autocomplete_whenSimilarAlias_thenReturnCorrectMatch() {
        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene1A", taxon, 10 );

        assertThat( matches ).hasSize( 1 );
        SearchResult<GeneInfo> match = matches.iterator().next();
        assertThat( match.getMatch() ).isEqualTo( genes.get( 1 ) );
        assertThat( match.getMatchType() ).isEqualTo( GeneMatchType.SIMILAR_ALIAS );
    }

    @Test
    public void autocomplete_whenMultipleMatches_thenReturnAll() {
        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene", taxon, 10 );

        assertThat( matches ).hasSize( 3 );
        assertThat( matches.stream().map( SearchResult::getMatch ).collect( Collectors.toSet() ) ).containsExactlyElementsOf( genes.values() );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toSet() ) ).containsExactly( GeneMatchType.SIMILAR_SYMBOL );
    }

    @Test
    public void autocomplete_whenMultipleMatchTypes_thenReturnCorrectOrder() {
        Map<Integer, GeneInfo> genes = new HashMap<>();
        genes.put( 10, createGene( 10, taxon ) );
        genes.put( 11, createGene( 11, taxon ) );
        genes.put( 12, createGene( 12, taxon ) );
        genes.put( 13, createGene( 13, taxon ) );

        genes.get( 10 ).setSymbol( "Match" );
        genes.get( 11 ).setSymbol( "MatchSimilar" );
        genes.get( 12 ).setName( "Match" );
        genes.get( 13 ).setAliases( "Match" );

        geneInfoRepository.save( genes.values() );

        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Match", taxon, 10 );

        assertThat( matches ).hasSize( 4 );
        assertThat( matches.stream().map( SearchResult::getMatch ).collect( Collectors.toList() ) )
                .containsExactly( genes.get( 10 ), genes.get( 11 ), genes.get( 12 ), genes.get( 13 ) );
        assertThat( matches.stream().map( SearchResult::getMatchType ).collect( Collectors.toList() ) )
                .containsExactly( GeneMatchType.EXACT_SYMBOL, GeneMatchType.SIMILAR_SYMBOL, GeneMatchType.SIMILAR_NAME, GeneMatchType.SIMILAR_ALIAS );
    }

    @Test
    public void autocomplete_whenMultipleMatchesAndResultsLimited_thenReturnSome() {
        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene", taxon, 2 );

        assertThat( matches ).hasSize( 2 );
    }

    @Test
    public void autocomplete_whenMultipleMatchesAndResultsUnlimited_thenReturnAll() {
        Collection<GeneInfo> geneInfos = IntStream.range( 10, 100 ).boxed().map(
                nbr -> {
                    GeneInfo g = createGene( nbr, taxon );
                    g.setSymbol( "Gene" + nbr );
                    return entityManager.persist( g );
                }
        ).collect( Collectors.toSet() );

        Collection<SearchResult<GeneInfo>> matches = geneService.autocomplete( "Gene", taxon, -1 );

        assertThat( matches.stream().map( sr -> sr.getMatch() ).collect( Collectors.toList() ) )
                .containsAll( genes.values() )
                .containsAll( geneInfos )
                .hasSize( 93 );
    }

    @Test
    public void deserializeGenes_whenMultipleValidOrInvalidIds_thenReturnValidGenes() {
        Map<Integer, TierType> geneTierMap = new HashMap<>();
        geneTierMap.put( 1, TierType.TIER1 );
        geneTierMap.put( 2, TierType.TIER2 );
        geneTierMap.put( 3, TierType.TIER3 );
        geneTierMap.put( 4, TierType.TIER3 );
        geneTierMap.put( 5, TierType.TIER3 );
        Map<GeneInfo, TierType> found = geneService.deserializeGenesTiers( geneTierMap );
        assertThat( found.keySet() ).containsExactly( genes.get( 1 ), genes.get( 2 ), genes.get( 3 ) );
        assertThat( found.values() ).containsExactly( TierType.TIER1, TierType.TIER2, TierType.TIER3 );
    }

    @Test
    public void updateGenes_thenSucceed() throws MalformedURLException, ParseException {
        Taxon humanTaxon = entityManager.persist( createTaxon( 9606 ) );
        humanTaxon.setGeneUrl( new URL( "ftp://ncbi/Homo_sapiens.gene_info.gz" ) );
        given( taxonService.findByActiveTrue() ).willReturn( Sets.newSet( humanTaxon ) );
        GeneInfo updatedGene = createGene( 4, humanTaxon );
        updatedGene.setSymbol( "FOO" );
        updatedGene.setName( "BAR" );
        given( geneInfoParser.parse( humanTaxon, humanTaxon.getGeneUrl() ) ).willReturn( Sets.newSet( updatedGene ) );
        geneService.updateGenes();
        verify( taxonService ).findByActiveTrue();
        assertThat( geneInfoRepository.findByGeneId( 4 ) )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "symbol", "FOO" )
                .hasFieldOrPropertyWithValue( "name", "BAR" )
                .hasFieldOrPropertyWithValue( "taxon", humanTaxon );
    }

}