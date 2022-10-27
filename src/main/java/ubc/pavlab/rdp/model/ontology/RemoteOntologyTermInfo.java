package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ubc.pavlab.rdp.model.RemoteResource;

import java.net.URI;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(of = { "ontology" }, callSuper = true)
public class RemoteOntologyTermInfo extends OntologyTermInfo implements RemoteResource {

    public static RemoteOntologyTermInfoBuilder<?, ?> builder( RemoteOntology ontology, String termId ) {
        return new RemoteOntologyTermInfoBuilderImpl().ontology( ontology ).termId( termId );
    }

    private RemoteOntology ontology;

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getOrigin() {
        return ontology.getOrigin();
    }

    @Override
    public void setOrigin( String origin ) {
        // FIXME: this should throw an unsupported operation exception
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public URI getOriginUrl() {
        return ontology.getOriginUrl();
    }

    @Override
    public void setOriginUrl( URI hostUrl ) {
        // FIXME: this should throw an unsupported operation exception
    }
}
