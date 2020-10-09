package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import ubc.pavlab.rdp.util.MatchType;

/**
 * Created by mjacobson on 30/01/18.
 */
@AllArgsConstructor
public enum TermMatchType implements MatchType {

    EXACT_ID( "Exact Id", 0 ),
    NAME_CONTAINS( "Name Contains", 1 ),
    DEFINITION_CONTAINS( "Definition Contains", 2 ),
    NAME_CONTAINS_PART( "Name Contains Part", 3 ),
    DEFINITION_CONTAINS_PART( "Definition Contains Part", 4 );

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
