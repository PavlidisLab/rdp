package ubc.pavlab.rdp.util;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OBO ontology parser
 */
@Component
@AllArgsConstructor
@CommonsLog
public class OBOParser {

    private static final Pattern DEFINITION_PATTERN = Pattern.compile( "\"(.+?)\" .*" );
    private static final Pattern SYNONYM_PATTERN = Pattern.compile( "\"(.+?)\" (.+?) .*" );

    @Data
    public static class Ontology {
        private String formatVersion;
        private String dataVersion;
        private String defaultNamespace;
        private String name;
    }

    /**
     * Term as defined by a [Term] record.
     */
    @Getter
    @Setter
    @ToString(of = { "id" })
    @EqualsAndHashCode(of = { "id" })
    public static class Term {

        @Data
        public static class Synonym {
            private final String synonym;
            private final String type;
        }

        @Data
        public static class Relationship {

            private final Term node;
            private final Typedef typedef;
        }

        private String id;
        private List<String> altIds = new ArrayList<>();
        private String namespace;
        private String name;
        private String definition;
        private List<Synonym> synonyms = new ArrayList<>();
        private Boolean obsolete;

        /* we map both directions of a relationship */
        private transient Set<Relationship> relationships = new HashSet<>();
        private transient Set<Relationship> inverseRelationships = new HashSet<>();

    }

    /**
     * Types of relationship as defined by [Typedef] records.
     */
    @Data
    @EqualsAndHashCode(of = { "id" })
    public static class Typedef {
        /**
         * This is not a term relationship as per OBO format, but rather defining a class hierarchy.
         */
        public static final Typedef IS_A = new Typedef( "is_a" );
        /**
         * http://purl.obolibrary.org/obo/BFO_0000050
         */
        public static final Typedef PART_OF = new Typedef( "part_of" );

        private final String id;
    }

    /**
     * Configuration for parsing OBO input.
     */
    @Data
    @Builder
    public static class Configuration {

        /**
         * Set of included relationships when parsing the OBO format.
         */
        @Singular
        Set<Typedef> includeTypedefs;
    }

    @Data
    public static class ParsingResult {

        private final Ontology ontology;
        private final List<Term> terms;

        /* conveniences */
        private final Map<String, Term> termsByIdOrAltId;
        private final Map<String, Typedef> typedefsById;
    }

    /**
     * Parse terms from a reader in the OBO format.
     *
     * @param reader a reader from which terms will be parsed
     * @return parsed terms
     * @throws IOException    if any I/O operation fails
     * @throws ParseException if any parsing fails
     */
    public ParsingResult parse( Reader reader, Configuration configuration ) throws IOException, ParseException {
        Ontology ontology = new Ontology();
        List<Term> terms = new ArrayList<>();
        List<Typedef> typedefs = new ArrayList<>();
        Map<String, Term> termMap = new HashMap<>();
        Map<String, Typedef> typedefMap = new HashMap<>();
        try ( LineNumberReader br = new LineNumberReader( reader ) ) {
            Term currentNode = null;
            Term parentNode;
            String line;
            Matcher m;
            while ( ( line = br.readLine() ) != null ) {
                if ( line.isEmpty() ) {
                    // Finished/Between Stanza
                    if ( currentNode != null ) {
                        // Just finished Term Stanza
                        // Create Node
                        terms.add( currentNode );
                        termMap.put( currentNode.getId(), currentNode );
                        currentNode = null;
                    } // Else Finished unimportant Stanza

                } else if ( line.equals( "[Term]" ) ) {
                    // check if the current node was closed properly
                    if ( currentNode != null ) {
                        throw new ParseException( String.format( "Previous node %s was not closed properly by an empty line.", currentNode ), br.getLineNumber() );
                    }
                    // Starting a Term Stanza
                    currentNode = new Term();
                } else if ( line.equals( "[Typedef]" ) ) {
                    // TODO
                } else if ( currentNode != null ) {
                    // Within a Term Stanza
                    String[] tagValuePair = line.split( ": ", 2 );
                    String[] values;
                    if ( tagValuePair.length < 2 ) {
                        throw new ParseException( MessageFormat.format( "Could not parse line: {0}.", line ), br.getLineNumber() );
                    }
                    switch ( tagValuePair[0] ) {
                        case "id":
                            if ( !termMap.containsKey( tagValuePair[1] ) ) {
                                currentNode.setId( tagValuePair[1] );
                            } else {
                                currentNode = termMap.get( tagValuePair[1] );
                            }
                            break;
                        case "name":
                            currentNode.setName( tagValuePair[1] );
                            break;
                        case "namespace":
                            currentNode.setNamespace( tagValuePair[1] );
                            break;
                        case "alt_id":
                            currentNode.altIds.add( tagValuePair[1] );
                            termMap.put( tagValuePair[1], currentNode );
                            break;
                        case "def":
                            m = DEFINITION_PATTERN.matcher( tagValuePair[1] );
                            if ( !m.matches() ) {
                                throw new ParseException( String.format( "Could not parse the right hand side %s of line: %s.", tagValuePair[1], line ), br.getLineNumber() );
                            }
                            currentNode.setDefinition( m.group( 1 ) );
                            break;
                        case "is_a":
                            values = tagValuePair[1].split( " " );
                            if ( !termMap.containsKey( values[0] ) ) {
                                // parent exists in map
                                parentNode = new Term();
                                parentNode.setId( values[0] );
                                termMap.put( values[0], parentNode );
                            } else {
                                parentNode = termMap.get( values[0] );
                            }
                            currentNode.getRelationships().add( new Term.Relationship( parentNode, Typedef.IS_A ) );
                            parentNode.getInverseRelationships().add( new Term.Relationship( currentNode, Typedef.IS_A ) );
                            break;
                        case "relationship":
                            values = tagValuePair[1].split( " " );
                            if ( values.length < 2 ) {
                                throw new ParseException( String.format( "Could not parse the right hand side %s of line: %s.", tagValuePair[1], line ), br.getLineNumber() );
                            }
                            String typedefId = values[0];
                            String termId = values[1];
                            if ( !typedefMap.containsKey( typedefId ) ) {
                                typedefMap.put( typedefId, new Typedef( typedefId ) );
                            }
                            Typedef typedef = typedefMap.get( typedefId );
                            if ( configuration.getIncludeTypedefs().contains( typedef ) ) {
                                if ( !termMap.containsKey( termId ) ) {
                                    // parent exists in map
                                    parentNode = new Term();
                                    parentNode.setId( termId );
                                    termMap.put( termId, parentNode );
                                } else {
                                    parentNode = termMap.get( values[1] );
                                }
                                currentNode.getRelationships().add( new Term.Relationship( parentNode, typedef ) );
                                parentNode.getInverseRelationships().add( new Term.Relationship( currentNode, typedef ) );
                            }
                            break;
                        case "is_obsolete":
                            currentNode.setObsolete( tagValuePair[1].equals( "true" ) );
                            break;
                        case "synonym":
                            m = SYNONYM_PATTERN.matcher( tagValuePair[1] );
                            if ( !m.matches() ) {
                                throw new ParseException( String.format( "Could not parse the right hand side %s of line: %s.", tagValuePair[1], line ), br.getLineNumber() );
                            }
                            currentNode.getSynonyms().add( new Term.Synonym( m.group( 1 ), m.group( 2 ) ) );
                        default:
                            break;
                    }
                } else {
                    // Within a Term Stanza
                    String[] tagValuePair = line.split( ": ", 2 );
                    if ( tagValuePair.length < 2 ) {
                        throw new ParseException( String.format( "Expected two parts: %s", line ), br.getLineNumber() );
                    }
                    switch ( tagValuePair[0] ) {
                        case "format-version":
                            ontology.setFormatVersion( tagValuePair[1] );
                            break;
                        case "data-version":
                            ontology.setDataVersion( tagValuePair[1] );
                            break;
                        case "default-namespace":
                            ontology.setDefaultNamespace( tagValuePair[1] );
                            break;
                        case "ontology":
                            ontology.setName( tagValuePair[1] );
                            break;
                    }
                }
            }

            // make sure that the last node is saved
            if ( currentNode != null ) {
                termMap.put( currentNode.getId(), currentNode );
            }
        }

        return new ParsingResult( ontology, terms, termMap, typedefMap );
    }

    /**
     * Default strategy for parsing OBO format which only considers the "is_a" hierarchy.
     *
     * @see #parse(Reader, Configuration)
     */
    public ParsingResult parse( Reader reader ) throws IOException, ParseException {
        return parse( reader, Configuration.builder().build() );
    }
}
