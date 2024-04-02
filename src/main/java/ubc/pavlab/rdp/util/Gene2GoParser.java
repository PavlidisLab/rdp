package ubc.pavlab.rdp.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.*;

/**
 * Read in the Gene2Go file provided by NCBI.
 * <p>
 * Created by mjacobson on 17/01/18.
 */
@CommonsLog
public class Gene2GoParser {

    private static final String TAXON_ID_FIELD = "#tax_id", GENE_ID_FIELD = "GeneID", GO_ID_FIELD = "GO_ID";
    private static final String[] EXPECTED_FIELDS = { TAXON_ID_FIELD, GENE_ID_FIELD, GO_ID_FIELD };
    private static final int
            TAXON_ID_INDEX = ArrayUtils.indexOf( EXPECTED_FIELDS, TAXON_ID_FIELD ),
            GENE_ID_INDEX = ArrayUtils.indexOf( EXPECTED_FIELDS, GENE_ID_FIELD ),
            GO_ID_INDEX = ArrayUtils.indexOf( EXPECTED_FIELDS, GO_ID_FIELD );

    @Nullable
    private final Set<Integer> retainedTaxa;

    /**
     * @param retainedTaxa a set of taxa to retain from the gene2go input, or null to ignore
     */
    public Gene2GoParser( @Nullable Set<Integer> retainedTaxa ) {
        this.retainedTaxa = retainedTaxa;
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Record {
        int taxonId;
        int geneId;
        String goId;
    }

    public Collection<Record> parse( InputStream input ) throws ParseException, IOException {
        StopWatch timer = StopWatch.createStarted();
        try ( LineNumberReader br = new LineNumberReader( new InputStreamReader( input ) ) ) {
            String headerLine = br.readLine();

            if ( headerLine == null ) {
                throw new ParseException( "Stream contains no data.", br.getLineNumber() );
            }

            String[] headerFields = headerLine.split( "\t" );

            for ( String field : EXPECTED_FIELDS ) {
                if ( !ArrayUtils.contains( headerFields, field ) ) {
                    throw new ParseException( String.format( "Unexpected header line: %s", headerLine ), br.getLineNumber() );
                }
            }

            String line;
            Set<Integer> seenTaxa = new HashSet<>();
            List<Record> records = new ArrayList<>();
            while ( ( line = br.readLine() ) != null ) {
                Record r;
                int lineNumber = br.getLineNumber();
                int taxonId, geneId;
                String goId;
                String[] values = line.split( "\t" );
                if ( values.length < headerFields.length ) {
                    throw new ParseException( String.format( "Unexpected number of parts in: %s", line ), lineNumber );
                }
                try {
                    taxonId = Integer.parseInt( values[TAXON_ID_INDEX] );
                    seenTaxa.add( taxonId );
                    if ( retainedTaxa != null && !retainedTaxa.contains( taxonId ) ) {
                        // we've seen all the taxa that we needed to, terminate
                        if ( seenTaxa.containsAll( retainedTaxa ) ) {
                            log.debug( "All taxa we needed were parsed, terminating early!" );
                            break;
                        }
                        continue;
                    } else {
                        geneId = Integer.parseInt( values[GENE_ID_INDEX] );
                        goId = values[GO_ID_INDEX];
                        r = new Record( taxonId, geneId, goId );
                    }
                } catch ( NumberFormatException e ) {
                    throw new ParseException( String.format( "Could not parse number for: %s", line ), lineNumber, e );
                } finally {
                    if ( ( lineNumber + 1 ) % 1000000 == 0 ) {
                        log.debug( String.format( "Parsed %d line from (%d line/s)",
                                lineNumber + 1, (int) ( 1000.0 * ( lineNumber + 1 ) / timer.getTime() ) ) );
                    }
                }
                records.add( r );
            }
            return records;
        }
    }
}
