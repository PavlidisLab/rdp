package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

@Getter
@AllArgsConstructor
public enum ResearcherCategory {
    IN_SILICO( "in-silico" ),
    IN_VITRO_BIOCHEMICAL( "in-vitro" ),
    IN_VITRO_CELLS( "in-vitro-cells" ),
    IN_VITRO_STRUCTURAL( "in-vitro-structural" ),
    IN_VIVO( "in-vivo" ),
    OTHER( "other" );
    private final String id;

    public MessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( "ResearcherCategory." + name() );
    }
}
