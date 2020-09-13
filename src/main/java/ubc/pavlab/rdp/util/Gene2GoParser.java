package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.Taxon;

import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Read in the Gene2Go file provided by NCBI.
 * <p>
 * Created by mjacobson on 17/01/18.
 */
@CommonsLog
@Component
public class Gene2GoParser {

    private static final String EXPECTED_HEADER = "#tax_id\tGeneID\tGO_ID\tEvidence\tQualifier\tGO_term\tPubMed\tCategory";

    @Data
    @AllArgsConstructor
    public class Record {
        private Integer taxonId;
        private Integer geneId;
        private String goId;
    }

    public void populateAnnotations( URL url, Collection<Taxon> acceptableTaxons ) throws ParseException {

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

            populateAnnotations( new GZIPInputStream( ftp.retrieveFileStream( url.getPath() ) ) );

        } catch ( IOException e ) {
            throw new ParseException( e.getMessage(), 0 );
        } finally {
            try {
                if ( ftp.isConnected() ) {
                    ftp.disconnect();
                }
            } catch ( IOException ex ) {
                log.error( ex.getMessage() );
            }
        }
    }

    public void populateAnnotations( File file ) throws ParseException {
        try {
            populateAnnotations( new GZIPInputStream( new FileInputStream( file ) ) );
        } catch ( IOException e ) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

    public Collection<Record> populateAnnotations( InputStream input ) throws ParseException {
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( input ) ) ) {
            String header = br.readLine();

            if ( header == null ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            if ( !header.equalsIgnoreCase( EXPECTED_HEADER ) ) {
                throw new ParseException( MessageFormat.format( "Unexpected header line: {0}.", header ), 0 );
            }

            return br.lines()
                    .map( line -> line.split( "\t" ) )
                    .map( values -> new Record( Integer.valueOf( values[0] ), Integer.valueOf( values[1] ), values[2] ) )
                    .collect( Collectors.toList() );
        } catch ( IOException e ) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

}
