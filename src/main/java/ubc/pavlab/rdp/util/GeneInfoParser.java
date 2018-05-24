package ubc.pavlab.rdp.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;

import java.io.*;
import java.net.URL;
import java.text.ParseException;
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

    public static Set<Gene> parse( Taxon taxon, URL url ) throws ParseException {

        String proxyHost = System.getProperty( "ftp.proxyHost" );

        FTPClient ftp;

        if ( proxyHost != null ) {
            Integer proxyPort = Integer.parseInt( System.getProperty( "ftp.proxyPort" ) );
            log.info( "Using HTTP proxy server: " + proxyHost + ":" + proxyPort.toString() );
            ftp = new FTPHTTPClient( proxyHost, proxyPort );
        } else {
            ftp = new FTPClient();
        }

        try {

            ftp.connect( url.getHost() );
            ftp.login( "anonymous", "" );
            ftp.enterLocalPassiveMode();
            ftp.setFileType( FTP.BINARY_FILE_TYPE );
            ftp.setBufferSize( 1024 * 1024 );

            return parse( taxon, new GZIPInputStream( ftp.retrieveFileStream( url.getPath() ) ) );

        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        } finally {
            try {
                if ( ftp.isConnected() ) {
                    ftp.disconnect();
                }
            } catch (IOException ex) {
                log.error( ex.getMessage() );
            }
        }
    }

    public static Set<Gene> parse( Taxon taxon, File file ) throws ParseException {
        try {
            return parse( taxon, new GZIPInputStream( new FileInputStream( file ) ) );
        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

    private static Set<Gene> parse( Taxon taxon, InputStream input ) throws ParseException {
        try {

            BufferedReader br = new BufferedReader( new InputStreamReader( input ) );

            String header = br.readLine();

            if ( header == null ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            if ( !header.equalsIgnoreCase( EXPECTED_HEADER ) ) {
                throw new ParseException( "Unexpected Header Line!", 0 );
            }

            return br.lines().map( line -> line.split( "\t" ) ).filter( values -> Integer.valueOf( values[0] ).equals( taxon.getId() ) ).map( values -> {
                Gene gene = new Gene();
                gene.setTaxon( taxon );
                gene.setGeneId( Integer.valueOf( values[1] ) );
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
                if ( input != null ) {
                    input.close();
                }
            } catch (IOException ex) {
                log.error( ex.getMessage() );
            }
        }
    }

}
