package ubc.pavlab.rdp.repositories;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;
import ubc.pavlab.rdp.util.TestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static ubc.pavlab.rdp.util.TestUtils.createTerm;

@RunWith(SpringRunner.class)
public class GeneOntologyTermInfoRepositoryTest {

    @TestConfiguration
    public static class GeneOntologyTermInfoRepositoryTestContextConfiguration {

        @Bean
        public GeneOntologyTermInfoRepository geneOntologyTermInfoRepository() {
            return new GeneOntologyTermInfoRepository();
        }
    }

    @Autowired
    private GeneOntologyTermInfoRepository geneOntologyTermInfoRepository;

    @After
    public void tearDown() {
        geneOntologyTermInfoRepository.deleteAll();
    }

    @Test
    public void findAll() {
        List<GeneOntologyTermInfo> terms = IntStream.range( 1, 10000 )
                .mapToObj( TestUtils::createTerm )
                .collect( Collectors.toList() );
        assertThat( geneOntologyTermInfoRepository.saveAll( terms ) )
                .hasSize( 9999 );
        assertThat( geneOntologyTermInfoRepository.findAll() )
                .containsExactlyInAnyOrderElementsOf( terms );
    }

    @Test
    public void findAllAsStream() {
        List<GeneOntologyTermInfo> terms = IntStream.range( 1, 10000 )
                .mapToObj( TestUtils::createTerm )
                .collect( Collectors.toList() );
        assertThat( geneOntologyTermInfoRepository.saveAll( terms ) )
                .hasSize( 9999 );
        Stream<GeneOntologyTermInfo> stream = geneOntologyTermInfoRepository.findAllAsStream();
        // ensure that the repository is available for read-only operations while the stream is open
        assertThat( geneOntologyTermInfoRepository.findAll() ).hasSize( 9999 );
        Future<?> future = Executors.newSingleThreadExecutor().submit( () -> {
            geneOntologyTermInfoRepository.save( createTerm( 100001 ) );
        } );
        // thread will block for write access
        assertThat( future ).failsWithin( 10, TimeUnit.MILLISECONDS );
        // until the stream is closed
        stream.close();
        assertThat( future ).succeedsWithin( 10, TimeUnit.MILLISECONDS );
    }

    @Test
    public void findById() {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( "GO:000001" );
        term.setDirectGeneIds( Collections.singleton( 1 ) );
        geneOntologyTermInfoRepository.saveByAlias( "GO:109202", term );
        assertThat( geneOntologyTermInfoRepository.findById( "GO:109202" ) ).get().isEqualTo( term );
    }

    @Test
    public void existsById_whenTermIsSavedByAlias_thenBothIdAndAliasCanBeUsed() {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( "GO:000001" );
        term.setDirectGeneIds( Collections.singleton( 1 ) );
        geneOntologyTermInfoRepository.saveByAlias( "GO:109203", term );
        assertThat( geneOntologyTermInfoRepository.existsById( "GO:000001" ) ).isTrue();
        assertThat( geneOntologyTermInfoRepository.existsById( "GO:109203" ) ).isTrue();
    }

    @Test
    public void delete() {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( "GO:000001" );
        term.setDirectGeneIds( Collections.singleton( 1 ) );
        geneOntologyTermInfoRepository.save( term );
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 1 );
        assertThat( geneOntologyTermInfoRepository.findById( "GO:000001" ) )
                .isEqualTo( Optional.of( term ) );
        assertThat( geneOntologyTermInfoRepository.findByDirectGeneIdsContaining( 1 ) )
                .containsExactly( term );
        geneOntologyTermInfoRepository.delete( term );
        assertThat( geneOntologyTermInfoRepository.findByDirectGeneIdsContaining( 1 ) )
                .isEmpty();
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 0 );
    }

    @Test
    public void delete_whenTermDoesNotExist() {
        GeneOntologyTermInfo term = createTerm( "GO:000001" );
        geneOntologyTermInfoRepository.delete( term );
    }

    @Test
    public void delete_whenTermWasSavedByAlias_thenAliasAreAlsoDeleted() {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( "GO:000001" );
        term.setDirectGeneIds( Collections.singleton( 1 ) );
        geneOntologyTermInfoRepository.saveByAlias( "GO:109203", term );
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 1 );
        assertThat( geneOntologyTermInfoRepository.findById( "GO:000001" ) )
                .isPresent();
        assertThat( geneOntologyTermInfoRepository.findById( "GO:109203" ) )
                .isPresent();
        geneOntologyTermInfoRepository.delete( term );
        assertThat( geneOntologyTermInfoRepository.findById( "GO:000001" ) )
                .isNotPresent();
        assertThat( geneOntologyTermInfoRepository.findById( "GO:109203" ) )
                .isNotPresent();
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 0 );
    }


    @Test
    public void deleteById() {
        GeneOntologyTermInfo term = new GeneOntologyTermInfo();
        term.setGoId( "GO:000001" );
        term.setDirectGeneIds( Collections.singleton( 1 ) );
        geneOntologyTermInfoRepository.save( term );
        geneOntologyTermInfoRepository.deleteById( term.getGoId() );
        assertThat( geneOntologyTermInfoRepository.findByDirectGeneIdsContaining( 1 ) )
                .isEmpty();
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 0 );
    }

    @Test
    public void delete_whenMultipleThreadCompete_thenRunInCorrectOrder() throws InterruptedException {
        List<GeneOntologyTermInfo> terms = IntStream.range( 1, 10000 )
                .mapToObj( TestUtils::createTerm )
                .collect( Collectors.toList() );
        ExecutorService executor = Executors.newFixedThreadPool( 4 );
        for ( int i = 0; i < 4; i++ ) {
            final int k = i;
            executor.submit( () -> {
                for ( int j = k; j < terms.size(); j += 4 ) {
                    assertThat( geneOntologyTermInfoRepository.existsById( terms.get( j ).getGoId() ) )
                            .isTrue();
                    geneOntologyTermInfoRepository.delete( terms.get( j ) );
                    assertThat( geneOntologyTermInfoRepository.existsById( terms.get( j ).getGoId() ) )
                            .isFalse();
                }
            } );
        }
        executor.shutdown();
        assertThat( executor.awaitTermination( 10, TimeUnit.SECONDS ) ).isTrue();
    }

    @Test
    public void deleteAll() {
        for ( int i = 0; i < 10000; i++ ) {
            geneOntologyTermInfoRepository.save( createTerm( i ) );
        }
        geneOntologyTermInfoRepository.deleteAll();
        assertThat( geneOntologyTermInfoRepository.findAll() ).isEmpty();
        assertThat( geneOntologyTermInfoRepository.count() ).isEqualTo( 0 );
    }
}