package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Created by mjacobson on 30/01/18.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = { "match" })
@ToString
public class SearchResult<T> {
    private final MatchType matchType;
    private final T match;
}
