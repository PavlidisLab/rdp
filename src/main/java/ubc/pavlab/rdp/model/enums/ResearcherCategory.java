package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResearcherCategory {
    IN_VIVO( "in-vivo" ),
    IN_VITRO_CELLS( "in-vitro-cells" ),
    IN_VITRO_BIOCHEMICAL( "in-vitro" ),
    IN_VITRO_STRUCTURAL( "in-vitro-structural" ),
    IN_SILICO( "in-silico" ),
    OTHER( "other" );
    private final String id;
}
