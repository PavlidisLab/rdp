package ubc.pavlab.rdp.util;

import lombok.Getter;

import java.text.MessageFormat;

/**
 * Exception raised when a parsing error occurs for biological data.
 */
public class ParseException extends Exception {

    @Getter
    private int lineNumber;

    public ParseException( String message, int lineNumber ) {
        super( MessageFormat.format( "{0}: {1}", lineNumber, message ) );
        this.lineNumber = lineNumber;
    }

    public ParseException( String message, int lineNumber, Throwable cause ) {
        super( MessageFormat.format( "{0}: {1}", lineNumber, message ), cause );
        this.lineNumber = lineNumber;
    }

}
