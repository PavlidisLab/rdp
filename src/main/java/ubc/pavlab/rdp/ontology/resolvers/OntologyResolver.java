package ubc.pavlab.rdp.ontology.resolvers;

import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;

import java.net.URI;

/**
 * @author poirigui
 */
public interface OntologyResolver {

    /**
     * Indicate if this resolver accepts the given ontology.
     */
    boolean accepts( Ontology ontology );

    /**
     * Retrieve a URI for an ontology.
     */
    URI resolveViewOntologyUri( Ontology ontology );

    /**
     * Retrieve a URI for a term.
     */
    URI resolveViewTermUri( OntologyTerm term );
}
