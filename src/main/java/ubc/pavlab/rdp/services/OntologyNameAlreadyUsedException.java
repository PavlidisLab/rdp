package ubc.pavlab.rdp.services;

import lombok.Getter;

@Getter
public class OntologyNameAlreadyUsedException extends Exception {

    /**
     * Ontology name at fault.
     */
    private final String ontologyName;

    public OntologyNameAlreadyUsedException( String ontologyName ) {
        super( String.format( "An ontology with the same name '%s' is already used.", ontologyName ) );
        this.ontologyName = ontologyName;
    }
}
