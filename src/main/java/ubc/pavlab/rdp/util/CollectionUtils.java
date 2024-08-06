package ubc.pavlab.rdp.util;

import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.function.Function.identity;
import static org.springframework.util.CollectionUtils.containsAny;

public class CollectionUtils {

    public static <T, K, V> Collector<T, ?, Map<K, V>> toNullableMap( Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper ) {
        return Collector.of( HashMap::new,
                ( m, v ) -> m.put( keyMapper.apply( v ), valueMapper.apply( v ) ),
                ( m1, m2 ) -> {
                    m1.putAll( m2 );
                    return m1;
                },
                identity() );
    }

    /**
     * Update a given collection to reflect the elements of another by adding and removing only the necessary elements.
     * <p>
     * This is useful to prevent replacing elements that are equal as per {@link Object#equals(Object)}, but not
     * identical.
     *
     * @param destination the collection that will be updated
     * @param to          the collection used to update elements in destination
     * @param <T>         the type of elements held in the collection
     */
    public static <T> void update( Collection<T> destination, Collection<T> to ) {
        destination.removeIf( e -> !to.contains( e ) );
        destination.addAll( to );
    }

    /**
     * Update a given collection as per {@link #update(Collection, Collection)}, but only applying to a subset of
     * elements that match a given condition.
     *
     * @param destination the collection that will be updated
     * @param to          the collection used to update elements in destination
     * @param condition   the condition elements in destination must meet to be updated
     * @param <T>         the type of elements held in the collection
     */
    public static <T> void updateIf( Collection<T> destination, Collection<T> to, Predicate<T> condition ) {
        destination.removeIf( e -> condition.test( e ) && !to.contains( e ) );
        destination.addAll( to );
    }

    /**
     * Update a given collection as per {@link #update(Collection, Collection)}, but using a mapping instead to resolve
     * the elements.
     *
     * @param destination the collection that will be updated
     * @param keySupplier supplies keys for the mapping given an element in destination
     * @param to          the mapping used to update elements in destination
     * @param <K>         the type of the keys in the mapping
     * @param <T>         the type of elements held in the collection
     */
    public static <K, T> void updateWithMap( Collection<T> destination, Function<T, K> keySupplier, Map<K, T> to ) {
        destination.removeIf( p -> !to.containsKey( keySupplier.apply( p ) ) );
        destination.addAll( to.values() );
    }

    /**
     * Extract a value before applying a predicate.
     */
    public static <T, U> Predicate<? super T> by( Function<? super T, ? extends U> supplier, Predicate<U> predicate ) {
        return u -> predicate.test( supplier.apply( u ) );
    }

    /**
     * Check if a source collection is null or contains at least one of the candidate elements.
     * <p>
     * This function uses a {@link Supplier} so that expensive candidates are only retrieved if the null-check and the
     * empty-check on the source collection are satisfied.
     *
     * @param source     a source collection where the candidates are expected to be found
     * @param candidates a supplier for the candidates
     * @return true if the source is null or contains at least one of the candidate elements; an empty source collection
     * will return false regardless of the supplied candidates
     * @see org.springframework.util.CollectionUtils#containsAny(Collection, Collection)
     */
    public static boolean nullOrContainsAtLeastOne( Collection<?> source, Supplier<Collection<?>> candidates ) {
        return source == null || containsAtLeastOne( source, candidates );
    }

    public static boolean containsAtLeastOne( @NonNull Collection<?> source, Supplier<Collection<?>> candidates ) {
        return ( !source.isEmpty() && containsAny( source, candidates.get() ) );
    }
}
