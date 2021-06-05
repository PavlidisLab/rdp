package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read in the Gene2Go file provided by NCBI.
 * <p>
 * Created by mjacobson on 17/01/18.
 */
@CommonsLog
@Component
public class Gene2GoParser {

    private static final String TAXON_ID_FIELD = "#tax_id", GENE_ID_FIELD = "GeneID", GO_ID_FIELD = "GO_ID";
    private static final String[] EXPECTED_FIELDS = { TAXON_ID_FIELD, GENE_ID_FIELD, GO_ID_FIELD };

    @Data
    @AllArgsConstructor
    public static class Record {
        private Integer taxonId;
        private Integer geneId;
        private String goId;

        public static Record parseLine( String line, String[] headerFields, int lineNumber ) throws UncheckedParseException {
            String[] values = line.split( "\t" );
            if ( values.length < headerFields.length ) {
                throw new UncheckedParseException( MessageFormat.format( "Unexpected number of parts in: {0}", line ), lineNumber );
            }
            try {
                return new Record( Integer.valueOf( values[ArrayUtils.indexOf( headerFields, TAXON_ID_FIELD )] ),
                        Integer.valueOf( values[ArrayUtils.indexOf( headerFields, GENE_ID_FIELD )] ),
                        values[ArrayUtils.indexOf( headerFields, GO_ID_FIELD )] );
            } catch ( NumberFormatException e ) {
                throw new UncheckedParseException( MessageFormat.format( "Could not parse number for: {0}.", line ), lineNumber, e );
            }
        }
    }

    public Collection<Record> parse( InputStream input ) throws ParseException, IOException {
        try ( LineNumberReader br = new LineNumberReader( new InputStreamReader( input ) ) ) {
            String headerLine = br.readLine();

            if ( headerLine == null ) {
                throw new ParseException( "Stream contains no data.", br.getLineNumber() );
            }

            String[] headerFields = headerLine.split( "\t" );

            for ( String field : EXPECTED_FIELDS ) {
                if ( !ArrayUtils.contains( headerFields, field ) ) {
                    throw new ParseException( MessageFormat.format( "Unexpected header line: {0}.", headerLine ), br.getLineNumber() );
                }
            }

            try {
                return br.lines()
                        .map( line -> Record.parseLine( line, headerFields, br.getLineNumber() ) )
                        .collect( Collectors.toList() );
            } catch ( UncheckedIOException ioe ) {
                throw ioe.getCause();
            } catch ( UncheckedParseException e ) {
                throw e.getCause();
            }
        }
    }
}
