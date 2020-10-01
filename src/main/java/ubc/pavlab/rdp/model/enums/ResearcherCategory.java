package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResearcherCategory {
    IN_SILICO( "in-silico", "In silico" ),
    IN_VITRO_BIOCHEMICAL( "in-vitro", "In vitro (biochemical)" ),
    IN_VITRO_CELLS( "in-vitro-cells", "In vitro (cells)" ),
    IN_VITRO_STRUCTURAL( "in-vitro-structural", "In vitro (structural)" ),
    IN_VIVO( "in-vivo", "In vivo" ),
    OTHER( "other", "Other" );
    private String id;
    private String label;
}
