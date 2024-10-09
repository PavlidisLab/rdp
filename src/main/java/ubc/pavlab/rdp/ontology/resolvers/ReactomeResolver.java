package ubc.pavlab.rdp.ontology.resolvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.services.ReactomeService;

import java.net.URI;

/**
 * Reactome Pathways resolver.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactomeResolver implements OntologyResolver {

    @Autowired
    private ReactomeService reactomeService;

    @Override
    public boolean accepts( Ontology ontology ) {
        Ontology reactome = reactomeService.findPathwaysOntology();
        return reactome != null && reactome.equals( ontology );
    }

    @Override
    public URI resolveViewOntologyUrl( Ontology ontology ) {
        return URI.create( "https://reactome.org/PathwayBrowser/" );
    }

    @Override
    public URI resolveViewTermUrl( OntologyTerm term ) {
        return UriComponentsBuilder.fromHttpUrl( "https://reactome.org/PathwayBrowser/#/{0}" )
                .build( term.getTermId() );
    }
}
