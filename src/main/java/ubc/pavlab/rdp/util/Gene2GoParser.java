package ubc.pavlab.rdp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 * <p>
 * Created by mjacobson on 17/01/18.
 */

public class Gene2GoParser {

    private static Log log = LogFactory.getLog( Gene2GoParser.class );

    private static final String EXPECTED_HEADER = "#tax_id\tGeneID\tGO_ID\tEvidence\tQualifier\tGO_term\tPubMed\tCategory";

    public static void populateAnnotations( String url, Collection<Taxon> acceptableTaxons, GeneService geneService, GOService goService ) throws ParseException, MalformedURLException {
        Map<Integer, Taxon> fastMap = acceptableTaxons.stream().collect( Collectors.toMap( Taxon::getId, t -> t) );
        URL u = new URL( url );

        FTPClient ftp = new FTPClient();

        try {

            ftp.connect(u.getHost());
            ftp.login("anonymous", "");
            ftp.enterLocalPassiveMode();
            ftp.setFileType( FTP.BINARY_FILE_TYPE);
            ftp.setBufferSize(1024*1024);

            BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream(ftp.retrieveFileStream(u.getPath())) ) );;

            String header = br.readLine();

            if ( header == null ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            if ( !header.equalsIgnoreCase( EXPECTED_HEADER ) ) {
                throw new ParseException( "Unexpected Header Line!", 0 );
            }

            br.lines().map( line -> line.split( "\t" ) ).filter( values -> fastMap.containsKey( Integer.valueOf(values[0]) ) ).forEach( values -> {

                GeneOntologyTerm term = goService.getTerm( values[2] );
                Gene gene = geneService.load( Integer.valueOf( values[1] ));

                term.getGenes().add( gene );
                gene.getTerms().add( term );

            } );

        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (IOException ex) {
                log.error( ex.getMessage() );
            }
        }
    }
}
