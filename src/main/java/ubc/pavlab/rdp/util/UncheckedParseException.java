package ubc.pavlab.rdp.util;

import lombok.Getter;

public class UncheckedParseException extends RuntimeException {

    @Getter
    private ParseException cause;

    protected UncheckedParseException( ParseException cause ) {
        super( cause );
        this.cause = cause;
    }

    public UncheckedParseException( String message, int lineNumber ) {
        this( new ParseException( message, lineNumber ) );
    }

    public UncheckedParseException( String message, int lineNumber, Throwable cause ) {
        this( new ParseException( message, lineNumber, cause ) );
    }
}
