package ubc.pavlab.rdp.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.services.OntologyService;

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

    private final static Pattern ONTOLOGY_DEFINITION = Pattern.compile( "^rdp\\.ontologies\\.([^.]*?)\\.definition$" );
    private final static Pattern ONTOLOGY_TERM_INFO_DEFINITION = Pattern.compile( "^rdp\\.ontologies\\.([^.]*?)\\.terms\\.([^.]*?)\\.definition$" );

    /**
     * FIXME: use {@link OntologyService}, but that creates a circular dependency since OntologyService depends on
     * MessageSource.
     */
    @Autowired
    private OntologyService ontologyService;

    @Override
    protected MessageFormat resolveCode( String code, Locale locale ) {
        return null;
    }

    @Override
    protected String resolveCodeWithoutArguments( String code, Locale locale ) {
        Matcher matcher;
        if ( ( matcher = ONTOLOGY_TERM_INFO_DEFINITION.matcher( code ) ).matches() ) {
            String ontologyName = matcher.group( 1 );
            String termName = matcher.group( 2 );
            return ontologyService.findDefinitionByTermNameAndOntologyName( termName, ontologyName ).orElse( null );
        } else if ( ( matcher = ONTOLOGY_DEFINITION.matcher( code ) ).matches() ) {
            String ontologyName = matcher.group( 1 );
            return ontologyService.findDefinitionByOntologyName( ontologyName ).orElse( null );
        } else {
            return super.resolveCodeWithoutArguments( code, locale );
        }
    }
}
