package ubc.pavlab.rdp.util;

import net.sf.ehcache.Ehcache;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;

/**
 * Utilities for working with {@link CacheManager} and {@link Cache}.
 *
 * @author poirigui
 */
public class CacheUtils {

    /**
     * Obtain a cache from a cache manager, failing with a {@link CacheMissingException} if unavailable.
     *
     * @param cacheManager cache manager
     * @param cacheName    name of the cache to initialize
     * @return the initialized cache, otherwise a {@link CacheMissingException} is raised
     * @throws CacheMissingException if the cache is missing from the cache manager
     */
    public static Cache getCache( CacheManager cacheManager, String cacheName ) throws CacheMissingException {
        Cache cache = cacheManager.getCache( cacheName );
        if ( cache == null ) {
            throw new CacheMissingException( String.format( "Cache %s is missing from %s.", cacheName, cacheManager ) );
        }
        return cache;
    }

    /**
     * Evict all the given keys from the cache.
     * <p>
     * This takes advantage of cache-specific collection eviction such as {@link Ehcache#removeAll(Collection)}.
     *
     * @param cache cache from which keys are evicte
     * @param keys  keys to be evicted
     */
    public static void evictAll( Cache cache, Collection<?> keys ) {
        if ( cache.getNativeCache() instanceof Ehcache ) {
            ( (Ehcache) cache.getNativeCache() ).removeAll( keys );
        } else {
            for ( Object key : keys ) {
                cache.evict( key );
            }
        }
    }
}
