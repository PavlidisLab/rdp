package ubc.pavlab.rdp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Line-oriented {@link Writer}.
 *
 * @author poirigui
 */
public class LineWriter extends BufferedWriter {
    public LineWriter( Writer out ) {
        super( out );
    }

    public void writeLine( String s ) throws IOException {
        write( s );
        newLine();
    }
}
