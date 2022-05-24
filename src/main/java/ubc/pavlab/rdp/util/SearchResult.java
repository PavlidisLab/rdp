package ubc.pavlab.rdp.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

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
public class SearchResult<T extends Comparable> {
    private final MatchType matchType;
    private final Integer id;
    private final String label;
    private final String description;
    private final String extras;
    @JsonIgnore
    private final T match;
}
