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
    private MatchType matchType;
    private T match;
}
