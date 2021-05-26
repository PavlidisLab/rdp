package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Component
public class GeneOrthologsParser {

    @Data
    @AllArgsConstructor
    public static class Record {
        private Integer taxonId;
        private Integer geneId;
        private String relationship;
        private Integer orthologTaxonId;
        private Integer orthologId;
    }

    public List<Record> parse( InputStream is ) throws IOException {
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( new GZIPInputStream( is ) ) ) ) {
            return br.lines()
                    .skip( 1 ) // skip the TSV header
                    .map( line -> line.split( "\t" ) )
                    .map( line -> new Record( Integer.parseInt( line[0] ),
                            Integer.parseInt( line[1] ),
                            line[2],
                            Integer.parseInt( line[3] ),
                            Integer.parseInt( line[4] ) ) )
                    .collect( Collectors.toList() );
        } catch ( UncheckedIOException ioe ) {
            throw ioe.getCause();
        }
    }
}
