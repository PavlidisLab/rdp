package ubc.pavlab.rdp.server.util;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.Result;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.expression.Criteria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implement a searchable cache by using Ehcache.
 * 
 * @author jleong
 * @version $Id$
 */
@Component
public abstract class SearchableEhcache<T> {
    @Autowired
    private CacheManager cacheManager;

    private Ehcache cache;

    public Collection<T> fetchByCriteria( Criteria criteria ) {
        net.sf.ehcache.search.Query query = this.cache.createQuery();
        query.includeValues();
        query.addCriteria( criteria );
        Results results = null;
        try {
            results = query.execute();
        } catch ( Exception e ) {
            throw new SearchException( "Query error" );
        }

        if ( results == null ) {
            return null;
        }

        Collection<T> genes = new HashSet<T>( results.size() );

        for ( Result result : results.all() ) {
            genes.add( ( T ) result.getValue() );
        }

        return genes;
    }

    public abstract String getCacheName();

    public abstract Object getKey( T object );

    public Attribute<Object> getSearchAttribute( String attributeName ) {
        return new Attribute<Object>( attributeName );// this.cache.getSearchAttribute( attributeName );
    }

    public boolean hasExpired() {
        // Causes all elements stored in the Cache to be synchronously checked for expiry (every 5 minutues), and if
        // expired, evicted.
        this.cache.evictExpiredElements();
        // Gets the size of the cache.This number is the actual number of elements, including expired elements that have
        // NOT BEEN REMOVED.
        return ( this.cache.getSize() <= 0 );
    }

    public boolean isKeyInCache( Object key ) {
        return this.cache.isKeyInCache( key );
    }

    public void putAll( Collection<T> objects ) {
        for ( T object : objects ) {
            this.cache.putIfAbsent( new Element( getKey( object ), object ) );
        }
    }

    public void removeAll() {
        this.cache.removeAll();
    }

    public int getSize() {
        return this.cache.getSize();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        this.cache = this.cacheManager.getCache( getCacheName() );
    }
}
