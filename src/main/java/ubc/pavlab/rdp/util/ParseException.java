package ubc.pavlab.rdp.util;

import lombok.Getter;

/**
 * Exception raised when a parsing error occurs for biological data.
 */
public class ParseException extends Exception {

    @Getter
    private int lineNumber;

    public ParseException( String message ) {
        super( message );
        this.lineNumber = 0;
    }

    public ParseException( String message, Throwable cause ) {
        super( message, cause );
        this.lineNumber = 0;
    }

    public ParseException( String message, int lineNumber ) {
        super( message );
        this.lineNumber = lineNumber;
    }

    public ParseException( String message, int lineNumber, Throwable cause ) {
        super( message, cause );
        this.lineNumber = lineNumber;
    }

}
