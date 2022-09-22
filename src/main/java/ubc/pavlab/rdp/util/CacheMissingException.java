package ubc.pavlab.rdp.util;

/**
 * Exception indicating that  a cache is missing from a {@link org.springframework.cache.CacheManager}.
 *
 * @author poirigui
 */
public class CacheMissingException extends RuntimeException {

    public CacheMissingException( String msg ) {
        super( msg );
    }
}
