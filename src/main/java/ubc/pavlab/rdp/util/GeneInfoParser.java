package ubc.pavlab.rdp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 * <p>
 * Created by mjacobson on 17/01/18.
 */

public class GeneInfoParser {

    private static Log log = LogFactory.getLog( GeneInfoParser.class );

    private static String EXPECTED_HEADER = "#tax_id\tGeneID\tSymbol\tLocusTag\tSynonyms\tdbXrefs\tchromosome\tmap_location\tdescription\ttype_of_gene\tSymbol_from_nomenclature_authority\tFull_name_from_nomenclature_authority\tNomenclature_status\tOther_designations\tModification_date\tFeature_type";


    private Set<Gene> data = new HashSet<>();

    public GeneInfoParser( Taxon taxon ) throws ParseException, MalformedURLException {

        URL url = new URL( taxon.getGeneUrl() );

        FTPClient ftp = new FTPClient();

        try {

            ftp.connect(url.getHost());
            ftp.login("anonymous", "");
            ftp.enterLocalPassiveMode();
            ftp.setFileType( FTP.BINARY_FILE_TYPE);
            ftp.setBufferSize(1024*1024);

            BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream(ftp.retrieveFileStream(url.getPath())) ) );;

            String header = br.readLine();

            if ( header == null ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            if ( !header.equalsIgnoreCase( EXPECTED_HEADER ) ) {
                throw new ParseException( "Unexpected Header Line!", 0 );
            }

            data = br.lines().map( line -> line.split( "\t" ) ).filter( values -> Integer.valueOf( values[0] ).equals( taxon.getId() ) ).map( values -> {
                Gene gene = new Gene();
                gene.setTaxon( taxon );
                gene.setId( Integer.valueOf( values[1] ) );
                gene.setSymbol( values[2] );
                gene.setAliases( values[4] );
                gene.setName( values[8] );
                gene.setModificationDate( Integer.valueOf( values[14] ) );
                return gene;
            } ).collect( Collectors.toSet() );
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

    public Set<Gene> getParsedData() {
        return data;
    }

}
