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
import ubc.pavlab.rdp.util.GOParser;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.GeneInfoParser;

import java.io.InputStream;
import java.net.URL;
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

    public static void main( String[] args ) {
        SpringApplication.run( Application.class, args );
    }

    @Override
    public void run( String... args ) throws Exception {

        log.info( "Application Start." );

        log.info( "Loading genes" );
        for ( Taxon taxon : taxonService.findByActiveTrue() ) {
            log.info( "Loading genes for " + taxon.toString() );
            Set<Gene> data = new GeneInfoParser( taxon ).getParsedData();
            log.info( "Done parsing." );
            geneService.addAll( data );
        }
        log.info( "Finished loading genes: " + geneService.size() );

        log.info( "Loading GO Terms" );
        InputStream input = new URL( GO_URL ).openStream();
        GOParser gOParser = new GOParser( input );
        goService.setTerms( gOParser.getMap() );
        log.info( "Gene Ontology loaded, total of " + goService.size() + " items." );

        log.info( "Loading Annotations" );
        Gene2GoParser.populateAnnotations( GENE2GO_URL, taxonService.findByActiveTrue(), geneService, goService );

        log.info( "Finished loading annotations" );

        for ( GeneOntologyTerm goTerm : goService.getAllTerms() ) {
            goTerm.setSizesByTaxon( goTerm.getGenes().stream().collect(
                    Collectors.groupingBy(
                            Gene::getTaxon, Collectors.counting()
                    ) ) );
        }

        log.info( "Finished precomputing gene annotation sizes" );


    }

}
