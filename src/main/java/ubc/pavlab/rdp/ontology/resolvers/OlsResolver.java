package ubc.pavlab.rdp.ontology.resolvers;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;

import java.net.URI;

/**
 * Resolve ontologies from <a href="https://www.ebi.ac.uk/ols/index">OLS</a>.
 *
 * @author poirigui
 */
@Component
@ConditionalOnProperty(name = "rdp.settings.ontology.default-resolver", havingValue = "ubc.pavlab.rdp.ontology.resolvers.OlsResolver")
public class OlsResolver implements OntologyResolver {

    private final static String DEFAULT_IRI_PREFIX = "http://purl.obolibrary.org/obo/";

    @Override
    public boolean accepts( Ontology ontology ) {
        return ontology.getOntologyUrl() != null && ontology.getOntologyUrl().toExternalForm().startsWith( DEFAULT_IRI_PREFIX );
    }

    @Override
    public URI resolveViewOntologyUrl( Ontology ontology ) {
        return UriComponentsBuilder.fromHttpUrl( "https://www.ebi.ac.uk/ols/ontologies/{0}" )
                .build( ontology.getName() );
    }

    @Override
    public URI resolveViewTermUrl( OntologyTerm term ) {
        return UriComponentsBuilder.fromHttpUrl( "https://www.ebi.ac.uk/ols/ontologies/{0}/terms" )
                .queryParam( "iri", "{1}" )
                .build( term.getOntology().getName(), DEFAULT_IRI_PREFIX + term.getTermId().replace( ':', '_' ) );
    }
}
