package ubc.pavlab.rdp.util;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Comparator;

/**
 * Created by mjacobson on 31/01/18.
 */
public interface MatchType {

    /**
     * Get a comparator for this match type which compares results by {@link #getOrder()}.
     */
    static Comparator<MatchType> getComparator() {
        return Comparator.comparingInt( MatchType::getOrder );
    }

    /**
     * Label used when displaying the matches.
     * <p>
     * This is used to serialize the match in JSON, thus the {@link JsonValue} annotation.
     */
    @JsonValue
    String getLabel();

    /**
     * Relative order to other match types.
     */
    int getOrder();
}
