package ubc.pavlab.rdp.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by mjacobson on 28/01/18.
 */
public enum Aspect {
    biological_process("BP"), cellular_component("CC"), molecular_function("MF");

    private String label;

    private Aspect( String label ) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
