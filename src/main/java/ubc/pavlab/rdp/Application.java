package ubc.pavlab.rdp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.settings.CacheSettings;
import ubc.pavlab.rdp.util.GOParser;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.GeneInfoParser;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableCaching
public class Application implements CommandLineRunner {

    private static Log log = LogFactory.getLog( Application.class );

    private static final String GENE2GO_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz";
    private static final String GO_URL = "http://purl.obolibrary.org/obo/go.obo";

    @Autowired
    TaxonService taxonService;

    @Autowired
    GeneService geneService;

    @Autowired
    GOService goService;

    @Autowired
    private CacheSettings cacheSettings;

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }

    @Override
    public void run( String... args ) throws Exception {

        log.info( "Application Start." );

        log.info( "Loading genes" );
        for ( Taxon taxon : taxonService.findByActiveTrue() ) {


            Set<Gene> data;
            if (cacheSettings.isLoadFromDisk()) {
                Path path = Paths.get(cacheSettings.getGeneFilesLocation(), taxon.getId() + ".gene_info.gz");
                log.info( "Loading genes for " + taxon.toString() + " from disk: " + path.toAbsolutePath() );
                data = GeneInfoParser.parse( taxon, path.toFile() );
            } else {
                log.info( "Loading genes for " + taxon.toString() + " from URL: " + taxon.getGeneUrl() );
                data = GeneInfoParser.parse( taxon, new URL( taxon.getGeneUrl() ) );
            }
            log.info( "Done parsing." );
            geneService.addAll( data );
        }
        log.info( "Finished loading genes: " + geneService.size() );

        if (cacheSettings.isLoadFromDisk()) {
            log.info( "Loading GO Terms from disk: " + cacheSettings.getTermFile() );
            goService.setTerms( GOParser.parse( new File( cacheSettings.getTermFile() ) ) );
        } else {
            log.info( "Loading GO Terms from URL: " + GO_URL );
            goService.setTerms( GOParser.parse( new URL( GO_URL ) ) );
        }

        log.info( "Gene Ontology loaded, total of " + goService.size() + " items." );


        if (cacheSettings.isLoadFromDisk()) {
            log.info( "Loading annotations from disk: " + cacheSettings.getAnnotationFile() );
            Gene2GoParser.populateAnnotations( new File( cacheSettings.getAnnotationFile() ), taxonService.findByActiveTrue(), geneService, goService );
        } else {
            log.info( "Loading annotations from URL: " + GENE2GO_URL );
            Gene2GoParser.populateAnnotations( new URL(GENE2GO_URL), taxonService.findByActiveTrue(), geneService, goService );
        }

        log.info( "Finished loading annotations" );

        for ( GeneOntologyTerm goTerm : goService.getAllTerms() ) {
            goTerm.setSizesByTaxon( goService.getGenes( goTerm ).stream().collect(
                    Collectors.groupingBy(
                            Gene::getTaxon, Collectors.counting()
                    ) ) );
        }

        log.info( "Finished precomputing gene annotation sizes" );


    }

}
