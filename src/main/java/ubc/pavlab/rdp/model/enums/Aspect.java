package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

/**
 * Created by mjacobson on 28/01/18.
 */
@AllArgsConstructor
public enum Aspect {
    biological_process( "BP" ), cellular_component( "CC" ), molecular_function( "MF" );

    private final String label;

    @JsonValue
    public String getLabel() {
        return label;
    }
}
