package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by mjacobson on 30/01/18.
 */
public enum MatchType {

    EXACT_SYMBOL( "Exact Symbol" ),
    SIMILAR_SYMBOL( "Similar Symbol" ),
    SIMILAR_NAME( "Similar Name" ),
    SIMILAR_ALIAS( "Similar Alias" );


    private String label;

    private MatchType( String label ) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
