package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.repositories.GeneInfoRepository;

import java.io.*;
import java.net.URL;
import java.sql.Date;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 * <p>
 * Created by mjacobson on 17/01/18.
 */

@Component
@CommonsLog
public class GeneInfoParser {

    private static final String EXPECTED_HEADER = "#tax_id\tGeneID\tSymbol\tLocusTag\tSynonyms\tdbXrefs\tchromosome\tmap_location\tdescription\ttype_of_gene\tSymbol_from_nomenclature_authority\tFull_name_from_nomenclature_authority\tNomenclature_status\tOther_designations\tModification_date\tFeature_type";

    @Autowired
    private GeneInfoRepository geneInfoRepository;

    public Set<GeneInfo> parse( Taxon taxon, URL url ) throws ParseException {

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

    public Set<GeneInfo> parse( Taxon taxon, File file ) throws ParseException {
        try {
            return parse( taxon, new GZIPInputStream( new FileInputStream( file ) ) );
        } catch (IOException e) {
            throw new ParseException( e.getMessage(), 0 );
        }
    }

    private Set<GeneInfo> parse( Taxon taxon, InputStream input ) throws ParseException {
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
                GeneInfo gene = geneInfoRepository.findByGeneIdAndTaxon( Integer.valueOf (values[1]), taxon );
                if (gene == null) {
                    gene = new GeneInfo();
                }
                gene.setTaxon( taxon );
                gene.setGeneId( Integer.valueOf( values[1] ) );
                gene.setSymbol( values[2] );
                gene.setAliases( values[4] );
                gene.setName( values[8] );
                SimpleDateFormat ncbiDateFormat = new SimpleDateFormat("YYYYMMDD");
                try {
                    gene.setModificationDate( ncbiDateFormat.parse( values[14] ) );
                } catch ( ParseException e ) {
                    log.warn( MessageFormat.format("Could not parse date {0} for gene {1}.", values[14], values[1]), e);
                }
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
