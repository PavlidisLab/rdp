package ubc.pavlab.rdp.util;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Represents an empty search result.
 * <p>
 * Unfortunately, the design of the autocomplete uses an array as root element which prevents us from using a different
 * payload to encode a lack of results. Instead, we need to create a dummy singleton list with a 'noresult' attribute.
 *
 * @param <T> the type of result that is expected
 * @author poirigui
 */
@Getter
public class EmptySearchResult<T extends Comparable<T>> extends SearchResult<T> {

    /**
     * @param label this contains the message to be displayed
     * @param value this contains the query, although it is unused in the frontend
     * @param <T>   the type of result that is expected
     * @return a singleton list with an empty result dummy object
     */
    public static <T extends Comparable<T>> List<SearchResult<T>> create( String label, String value ) {
        return Collections.singletonList( new EmptySearchResult<>( label, value ) );
    }

    @SuppressWarnings("SpellCheckingInspection")
    private final boolean noresults = true;

    private final String value;

    private EmptySearchResult( String label, String value ) {
        super( null, null, label, null, null );
        this.value = value;
    }
}
