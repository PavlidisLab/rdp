package ubc.pavlab.rdp.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

@Getter
@AllArgsConstructor
public enum ResearcherPosition {
    PRINCIPAL_INVESTIGATOR;

    public MessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( "ResearcherPosition." + name() );
    }
}
