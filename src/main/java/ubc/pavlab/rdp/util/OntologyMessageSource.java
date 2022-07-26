package ubc.pavlab.rdp.util;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.services.OntologyService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves various ontology-related message codes.
 * <p>
 * For now, this only resolve term definitions and external URL to view a term following the 'rdp.ontologies.{ontologyName}.view-term-url-pattern' and 'rdp.ontologies.{ontologyName}'
 *
 * @author poirigui
 */
@Component
public class OntologyMessageSource extends AbstractMessageSource {

    private final static Pattern ONTOLOGY_VIEW_TERM_URL = Pattern.compile( "^rdp\\.ontologies\\..*?\\.view-term-url-pattern$" );

    private final static Pattern ONTOLOGY_TERM_INFO_DEFINITION = Pattern.compile( "^rdp\\.ontologies\\.(.*?)\\.terms\\.(.*?).definition$" );
    private final static String DEFAULT_URL_ENCODED_IRI_PREFIX;

    static {
        try {
            DEFAULT_URL_ENCODED_IRI_PREFIX = URLEncoder.encode( "http://purl.obolibrary.org/obo/", "UTF-8" );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * FIXME: use {@link OntologyService}, but that creates a circular dependency since OntologyService depends on
     * MessageSource.
     */
    @Autowired
    private OntologyService ontologyService;


    @Override
    protected MessageFormat resolveCode( String code, Locale locale ) {
        Matcher matcher = ONTOLOGY_VIEW_TERM_URL.matcher( code );
        if ( matcher.matches() ) {
            return new MessageFormat( UriComponentsBuilder.fromHttpUrl( "https://www.ebi.ac.uk/ols/ontologies/{0}/terms" )
                    .queryParam( "iri", DEFAULT_URL_ENCODED_IRI_PREFIX + "{1}" )
                    .build()
                    .toUriString(), locale );
        }
        /* unresolved */
        return null;
    }

    @Override
    protected String resolveCodeWithoutArguments( String code, Locale locale ) {
        Matcher matcher = ONTOLOGY_TERM_INFO_DEFINITION.matcher( code );
        if ( matcher.matches() ) {
            String ontologyName = matcher.group( 1 );
            String termName = matcher.group( 2 );
            return ontologyService.findDefinitionByTermNameAndOntologyName( termName, ontologyName );
        }
        return super.resolveCodeWithoutArguments( code, locale );
    }
}
