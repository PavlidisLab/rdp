package ubc.pavlab.rdp.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 *
 * Created by mjacobson on 17/01/18.
 */

public class GOParser {

    private Map<String, GOTerm> termMap = new HashMap<>();

    public GOParser( InputStream i ) throws IOException {
        if ( i.available() == 0 ) {
            throw new IOException( "Stream contains no data." );
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( i ) );
        GOTerm currentNode = null;
        GOTerm parentNode;
        // while ( br.ready() ) {
        String line;
        while ((line = br.readLine()) != null) {
            // String line = br.readLine();

            if ( line.equals( "" ) ) {
                // Finished/Between Stanza
                if ( currentNode != null ) {
                    // Just finished Term Stanza
                    // Create Node
                    termMap.put( currentNode.getId(), currentNode );
                    currentNode = null;
                } // Else Finished unimportant Stanza

            } else if ( line.equals( "[Term]" ) ) {
                // Starting a Term Stanza
                currentNode = new GOTerm();
            } else if ( currentNode != null ) {
                // Within a Term Stanza
                String[] tagValuePair = line.split( ": ", 2 );
                String[] values;
                switch (tagValuePair[0]) {
                    case "id":
                        if ( !termMap.containsKey( tagValuePair[1] ) ) {
                            currentNode.setId( tagValuePair[1] );
                        } else {
                            currentNode = termMap.get( tagValuePair[1] );
                        }

                        break;
                    case "name":
                        currentNode.setTerm( tagValuePair[1] );
                        break;
                    case "namespace":
                        currentNode.setAspect( tagValuePair[1] );
                        break;
                    case "def":
                        currentNode.setDefinition( tagValuePair[1].split( "\"" )[1] );
                        break;
                    case "is_a":
                        values = tagValuePair[1].split( " " );
                        currentNode.addParent( new GOTerm.Relationship( values[0],
                                GOTerm.Relationship.RelationshipType.IS_A ) );
                        if ( !termMap.containsKey( values[0] ) ) {
                            // parent exists in map
                            parentNode = new GOTerm();
                            parentNode.setId( values[0] );
                            termMap.put( values[0], parentNode );
                        } else {
                            parentNode = termMap.get( values[0] );
                        }
                        parentNode.addChild( new GOTerm.Relationship( currentNode.getId(),
                                GOTerm.Relationship.RelationshipType.IS_A ) );
                        break;
                    case "relationship":
                        values = tagValuePair[1].split( " " );
                        if ( values[0].equals( "part_of" ) ) {
                            currentNode.addParent( new GOTerm.Relationship( values[1],
                                    GOTerm.Relationship.RelationshipType.PART_OF ) );
                            if ( !termMap.containsKey( values[1] ) ) {
                                // parent exists in map
                                parentNode = new GOTerm();
                                parentNode.setId( values[1] );
                                termMap.put( values[1], parentNode );
                            } else {
                                parentNode = termMap.get( values[1] );
                            }
                            parentNode.addChild( new GOTerm.Relationship( currentNode.getId(),
                                    GOTerm.Relationship.RelationshipType.PART_OF ) );
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
        br.close();
        i.close();

    }

    public Map<String, GOTerm> getMap() {
        return termMap;
    }

}
