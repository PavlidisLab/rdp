package ubc.pavlab.rdp.util;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.search.Results;
import net.sf.ehcache.search.expression.Criteria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 26/01/18.
 */
public abstract class SearchableEhcache<K, T> {

    private static Log log = LogFactory.getLog( SearchableEhcache.class );

    public abstract Ehcache getCache();

    public abstract K getKey( T object );

    public T fetchByKey( K key ) {
        Element res = getCache().get( key );
        if (res != null) {
            return (T) res.getObjectValue();
        }
        return null;
    }

    public Collection<T> fetchByKey( Collection<K> keys ) {
        return getCache().getAll( keys ).values().stream().map(e -> e != null ? (T) e.getObjectValue() : null).collect( Collectors.toList() );
    }

    public Collection<T> fetchByCriteria( Criteria criteria ) {
        Results results = getCache().createQuery().includeValues().addCriteria( criteria ).execute();
        if ( results == null ) {
            return null;
        }

        return results.all().stream().map( r -> (T) r.getValue() ).collect( Collectors.toSet() );
    }

    public T fetchOneByCriteria( Criteria criteria) {
        Collection<T> results = fetchByCriteria( criteria );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple matches for criteria, return first hit: " + criteria.toString() );
        }
        return results.iterator().next();
    }

    public void putAll( Collection<T> objects ) {
        getCache().putAll( objects.stream().map( o -> new Element( getKey(o), o ) ).collect( Collectors.toList()) );
    }

    public void removeAll() {
        getCache().removeAll();
    }
}
