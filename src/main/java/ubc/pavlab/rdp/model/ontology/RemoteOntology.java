package ubc.pavlab.rdp.model.ontology;

import lombok.*;
import lombok.experimental.SuperBuilder;
import ubc.pavlab.rdp.model.RemoteResource;

import java.net.URI;

/**
 * Represents an ontology stored in a partner repository.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(of = { "originUrl" }, callSuper = true)
@ToString(of = { "origin", "originUrl" }, callSuper = true)
public class RemoteOntology extends Ontology implements RemoteResource {

    public static RemoteOntologyBuilder<?, ?> builder( @NonNull String name ) {
        return new RemoteOntologyBuilderImpl().name( name );
    }

    private String origin;

    private URI originUrl;
}
