package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Parse NCBI gene orthologs format.
 *
 * @author guillaume
 */
@CommonsLog
@Component
public class GeneOrthologsParser {

    private static final String TAXON_ID_FIELD = "#tax_id",
            GENE_ID_FIELD = "GeneID",
            RELATIONSHIP_FIELD = "relationship",
            ORTHOLOG_TAXON_ID_FIELD = "Other_tax_id",
            ORTHOLOG_ID_FIELD = "Other_GeneID";
    private static final String[] EXPECTED_FIELDS = { TAXON_ID_FIELD, GENE_ID_FIELD, RELATIONSHIP_FIELD, ORTHOLOG_TAXON_ID_FIELD, ORTHOLOG_ID_FIELD };

    @Data
    @AllArgsConstructor
    public static class Record {
        private Integer taxonId;
        private Integer geneId;
        private String relationship;
        private Integer orthologTaxonId;

        private Integer orthologId;

        public static Record parseLine( String line, String[] header, int lineNumber ) throws UncheckedParseException {
            String[] pieces = line.split( "\t" );
            if ( pieces.length < header.length ) {
                throw new UncheckedParseException( "Unexpected number of fields.", lineNumber );
            }
            try {
                return new Record( Integer.parseInt( pieces[ArrayUtils.indexOf( header, TAXON_ID_FIELD )] ),
                        Integer.parseInt( pieces[ArrayUtils.indexOf( header, GENE_ID_FIELD )] ),
                        pieces[ArrayUtils.indexOf( header, RELATIONSHIP_FIELD )],
                        Integer.parseInt( pieces[ArrayUtils.indexOf( header, ORTHOLOG_TAXON_ID_FIELD )] ),
                        Integer.parseInt( pieces[ArrayUtils.indexOf( header, ORTHOLOG_ID_FIELD )] ) );
            } catch ( NumberFormatException e ) {
                throw new UncheckedParseException( "Could not parse number.", lineNumber, e );
            }
        }
    }

    public List<Record> parse( InputStream is ) throws ParseException, IOException {
        try ( LineNumberReader br = new LineNumberReader( new InputStreamReader( is ) ) ) {
            String[] header = br.readLine().split( "\t" );
            for ( String expectedField : EXPECTED_FIELDS ) {
                if ( !ArrayUtils.contains( header, expectedField ) ) {
                    throw new ParseException( "Header is missing the following field: " + expectedField, br.getLineNumber() );
                }
            }
            return br.lines()
                    .map( line -> Record.parseLine( line, header, br.getLineNumber() ) )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );
        } catch ( UncheckedIOException ioe ) {
            throw ioe.getCause();
        } catch ( UncheckedParseException e ) {
            throw e.getCause();
        }
    }
}
