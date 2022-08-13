package ubc.pavlab.rdp.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Comparator;

/**
 * Represents a search result.
 * <p>
 * Results are sorted first by match type and then by the match itself.
 * <p>
 * Results are rendered as "{label}: {description} (<i>{extras}</i>)", extras being optional.
 * <p>
 * Created by mjacobson on 30/01/18.
 */
@Data
@EqualsAndHashCode(of = { "match" })
public class SearchResult<T extends Comparable<T>> implements Comparable<SearchResult<T>> {

    private final MatchType matchType;
    /**
     * A unique, internal ID that disambiguate results with the same {@link #label}. This is not being displayed.
     */
    private final Integer id;
    /**
     * A label to identify the result.
     */
    private final String label;
    /**
     * A short description for the result.
     */
    private final String description;
    /**
     * The result itself.
     */
    private final T match;

    /**
     * Extra information which not all results might have.
     */
    private String extras;
    /**
     * A score for the search.
     */
    private double score;

    @Override
    public int compareTo( SearchResult<T> other ) {
        return Comparator.comparing( SearchResult<T>::getMatchType, MatchType.getComparator() )
                .thenComparing( SearchResult::getMatch )
                .compare( this, other );
    }
}
