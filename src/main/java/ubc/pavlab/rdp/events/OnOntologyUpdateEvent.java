package ubc.pavlab.rdp.events;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import ubc.pavlab.rdp.model.ontology.Ontology;

@Getter
@EqualsAndHashCode(callSuper = false)
public class OnOntologyUpdateEvent extends ApplicationEvent {

    private final Ontology ontology;

    public OnOntologyUpdateEvent( Ontology source ) {
        super( source );
        this.ontology = source;
    }
}
