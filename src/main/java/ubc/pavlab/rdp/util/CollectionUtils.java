package ubc.pavlab.rdp.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

import static java.util.function.Function.identity;

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

}
