package ubc.pavlab.rdp.ontology.resolvers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;

import java.net.URI;
import java.util.List;

/**
 * @author poirigui
 */
@Component("ontologyResolver")
@Order(PriorityOrdered.HIGHEST_PRECEDENCE)
@CommonsLog
public class OntologyResolverImpl implements OntologyResolver {

    @Autowired
    private List<? extends OntologyResolver> resolvers;

    @Override
    public boolean accepts( Ontology ontology ) {
        return resolvers.stream().anyMatch( r -> r.accepts( ontology ) );
    }

    @Override
    public URI resolveViewOntologyUri( Ontology ontology ) {
        for ( OntologyResolver resolver : resolvers ) {
            if ( resolver.accepts( ontology ) ) {
                return resolver.resolveViewOntologyUri( ontology );
            }
        }
        return null;
    }

    @Override
    public URI resolveViewTermUri( OntologyTerm term ) {
        for ( OntologyResolver resolver : resolvers ) {
            if ( resolver.accepts( term.getOntology() ) ) {
                return resolver.resolveViewTermUri( term );
            }
        }
        return null;
    }
}