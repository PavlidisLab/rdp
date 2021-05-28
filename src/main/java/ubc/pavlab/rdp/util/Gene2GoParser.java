package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.stream.Collectors;

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
    public static class Record {
        private Integer taxonId;
        private Integer geneId;
        private String goId;
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

            try {
                return br.lines()
                        .map( line -> line.split( "\t" ) )
                        .map( values -> new Record( Integer.valueOf( values[0] ), Integer.valueOf( values[1] ), values[2] ) )
                        .collect( Collectors.toList() );
            } catch ( UncheckedIOException ioe ) {
                throw ioe.getCause();
            }
        } catch ( IOException e ) {
            throw new ParseException( e.getMessage() );
        }
    }

}
