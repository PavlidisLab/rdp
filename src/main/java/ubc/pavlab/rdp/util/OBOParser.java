package ubc.pavlab.rdp.util;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * OBO ontology parser
 */
@Component
@AllArgsConstructor
@CommonsLog
public class OBOParser {

    /**
     * Term as defined by a [Term] record.
     */
    @Getter
    @Setter
    @ToString(of = { "id" })
    @EqualsAndHashCode(of = { "id" })
    public static class Term {

        @Data
        public static class Relationship {

            private final Term node;
            private final Typedef typedef;
        }

        private String id;
        private String namespace;
        private String name;
        private String definition;
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

    /**
     * Parse terms from a reader in the OBO format.
     *
     * @param reader a reader from which terms will be parsed
     * @return parsed terms
     * @throws IOException    if any I/O operation fails
     * @throws ParseException if any parsing fails
     */
    public Map<String, Term> parse( Reader reader, Configuration configuration ) throws IOException, ParseException {
        Map<String, Term> termMap = new HashMap<>();
        Map<String, Typedef> typedefMap = new HashMap<>();
        try ( LineNumberReader br = new LineNumberReader( reader ) ) {
            Term currentNode = null;
            Term parentNode;
            String line;
            while ( ( line = br.readLine() ) != null ) {
                if ( line.isEmpty() ) {
                    // Finished/Between Stanza
                    if ( currentNode != null ) {
                        // Just finished Term Stanza
                        // Create Node
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
                            termMap.put( tagValuePair[1], currentNode );
                            break;
                        case "def":
                            currentNode.setDefinition( tagValuePair[1].split( "\"" )[1] );
                            break;
                        case "is_a":
                            values = tagValuePair[1].split( " " );
                            if ( values.length < 2 ) {
                                throw new ParseException( String.format( "Could not parse the right hand side %s of line: %s.", tagValuePair[1], line ), br.getLineNumber() );
                            }
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
                        default:
                            break;
                    }
                }
            }

            // make sure that the last node is saved
            if ( currentNode != null ) {
                termMap.put( currentNode.getId(), currentNode );
            }
        }

        return termMap;
    }

    /**
     * Default strategy for parsing OBO format which only considers the "is_a" hierarchy.
     *
     * @see #parse(Reader, Configuration)
     */
    public Map<String, Term> parse( Reader reader ) throws IOException, ParseException {
        return parse( reader, Configuration.builder().build() );
    }
}
