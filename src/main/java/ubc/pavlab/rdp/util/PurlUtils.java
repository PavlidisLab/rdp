package ubc.pavlab.rdp.util;

import java.net.URL;

/**
 * Utilities to operate with <a href="https://obofoundry.org/principles/fp-003-uris.html">PURL</a>.
 */
public class PurlUtils {

    private static final String PURL_PREFIX = "http://purl.obolibrary.org";

    public static boolean isPurl( String url ) {
        return url.startsWith( PURL_PREFIX );
    }

    public static boolean isPurl( URL url ) {
        return isPurl( url.toExternalForm() );
    }
}
