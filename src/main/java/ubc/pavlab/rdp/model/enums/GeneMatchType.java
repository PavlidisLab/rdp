package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import ubc.pavlab.rdp.util.MatchType;

/**
 * Created by mjacobson on 30/01/18.
 */
@AllArgsConstructor
public enum GeneMatchType implements MatchType {

    EXACT_SYMBOL( "Exact Symbol", 0 ),
    SIMILAR_SYMBOL( "Similar Symbol", 1 ),
    SIMILAR_NAME( "Similar Name", 2 ),
    SIMILAR_ALIAS( "Similar Alias", 3 );


    private final String label;
    private final int order;

    @JsonValue
    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }
}
