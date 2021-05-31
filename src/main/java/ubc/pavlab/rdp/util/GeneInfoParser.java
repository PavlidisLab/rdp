package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.ArrayUtils.indexOf;

/**
 * Read in gene info provided by the NCBI.
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class GeneInfoParser {

    private static final String[] EXPECTED_HEADER_FIELDS = { "#tax_id", "GeneID", "Symbol", "Synonyms", "description", "Modification_date" };

    private static DateFormat NCBI_DATE_FORMAT = new SimpleDateFormat( "yyyyMMdd" );

    public List<Record> parse( InputStream input, Integer taxonId ) throws ParseException, IOException {
        try ( LineNumberReader br = new LineNumberReader( new InputStreamReader( input ) ) ) {
            String headerLine = br.readLine();

            if ( headerLine == null ) {
                throw new ParseException( "Stream contains no data.", br.getLineNumber() );
            }

            String[] header = headerLine.split( "\t" );

            for ( String expectedField : EXPECTED_HEADER_FIELDS ) {
                if ( !ArrayUtils.contains( header, expectedField ) ) {
                    throw new ParseException( MessageFormat.format( "Gene information is missing the following field: {0}.", expectedField ), br.getLineNumber() );
                }
            }

            try {
                return br.lines()
                        .map( line -> Record.parseLine( line, header, br.getLineNumber() ) )
                        .filter( record -> record.getTaxonId().equals( taxonId ) )
                        .collect( Collectors.toList() );
            } catch ( UncheckedIOException ioe ) {
                throw ioe.getCause();
            } catch ( UncheckedParseException e ) {
                throw e.getCause();
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class Record {

        private Integer taxonId;
        private Integer GeneId;
        private String symbol;
        private String synonyms;
        private String description;
        private Date modificationDate;

        public static Record parseLine( String line, String[] header, int lineNumber ) throws UncheckedParseException {
            String[] values = line.split( "\t" );
            if ( values.length < header.length ) {
                throw new UncheckedParseException( "Line does not have the expected number of fields.", lineNumber );
            }
            Integer taxonId;
            try {
                taxonId = Integer.parseInt( values[indexOf( header, "#tax_id" )] );
            } catch ( NumberFormatException e ) {
                throw new UncheckedParseException( "Could not parse taxon id.", lineNumber );
            }
            Integer geneId;
            try {
                geneId = Integer.parseInt( values[indexOf( header, "GeneID" )] );
            } catch ( NumberFormatException e ) {
                throw new UncheckedParseException( "Could not parse gene id.", lineNumber );
            }
            String symbol = values[indexOf( header, "Symbol" )];
            String synonyms = values[indexOf( header, "Synonyms" )];
            String description = values[indexOf( header, "description" )];
            Date modificationDate = null;
            try {
                modificationDate = NCBI_DATE_FORMAT.parse( values[indexOf( header, "Modification_date" )] );
            } catch ( java.text.ParseException e ) {
                log.warn( MessageFormat.format( "Malformed date for gene {0} in taxon {1}, value will be ignored.", geneId, taxonId ), e );
            }
            return new Record( taxonId, geneId, symbol, synonyms, description, modificationDate );
        }
    }
}
