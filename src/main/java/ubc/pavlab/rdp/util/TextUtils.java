package ubc.pavlab.rdp.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextUtils {

    public static List<String> tokenize( String query ) {
        return Arrays.stream( query.replaceAll( "[^\\w-:]+", " " ).split( "\\s+" ) )
                .map( String::toUpperCase )
                .collect( Collectors.toList() );
    }

    public static String normalize( String query ) {
        return String.join( " ", tokenize( query ) );
    }
}
