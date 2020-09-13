package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResearcherPosition {
    PRINCIPAL_INVESTIGATOR( "Principal Investigator" );
    private String label;
}
