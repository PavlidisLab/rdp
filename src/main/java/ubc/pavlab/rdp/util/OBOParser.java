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

    @Getter
    @Setter
    @ToString(of = { "id" })
    @EqualsAndHashCode(of = { "id" })
    public class Term {

        private String id;
        private String namespace;
        private String name;
        private String definition;
        private Boolean obsolete;
        private transient Set<Relationship> parents = new HashSet<>();
        private transient Set<Relationship> children = new HashSet<>();
    }

    @Data
    public class Relationship {

        private final Term node;
        private final RelationshipType relationshipType;
    }

    public enum RelationshipType {
        IS_A,
        PART_OF

    }

    public Map<String, Term> parseStream( InputStream input ) throws IOException, ParseException {
        Map<String, Term> termMap = new HashMap<>();

        try ( LineNumberReader br = new LineNumberReader( new InputStreamReader( input ) ) ) {
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
                            if ( !termMap.containsKey( values[0] ) ) {
                                // parent exists in map
                                parentNode = new Term();
                                parentNode.setId( values[0] );
                                termMap.put( values[0], parentNode );
                            } else {
                                parentNode = termMap.get( values[0] );
                            }
                            currentNode.getParents().add( new Relationship( parentNode, RelationshipType.IS_A ) );
                            parentNode.getChildren().add( new Relationship( currentNode, RelationshipType.IS_A ) );
                            break;
                        case "relationship":
                            values = tagValuePair[1].split( " " );
                            if ( values[0].equals( "part_of" ) ) {

                                if ( !termMap.containsKey( values[1] ) ) {
                                    // parent exists in map
                                    parentNode = new Term();
                                    parentNode.setId( values[1] );
                                    termMap.put( values[1], parentNode );
                                } else {
                                    parentNode = termMap.get( values[1] );
                                }
                                currentNode.getParents().add( new Relationship( parentNode, RelationshipType.PART_OF ) );
                                parentNode.getParents().add( new Relationship( currentNode, RelationshipType.PART_OF ) );
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
}
