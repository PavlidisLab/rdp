package ubc.pavlab.rdp.util;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Comparator;

/**
 * Represents a search result.
 * <p>
 * Results are sorted first by match type and then by the match itself.
 * <p>
 * Results are rendered as "{label}: {description} (<i>{extras}</i>) [{attributes}]", extras being optional.
 * <p>
 * Created by mjacobson on 30/01/18.
 */
@Data
@EqualsAndHashCode(of = { "match" })
public class SearchResult<T extends Comparable<T>> {

    private final MatchType matchType;
    private final Integer id;
    private final String label;
    private final String description;
    private final String extras;
    private final T match;
    private double score;
}
