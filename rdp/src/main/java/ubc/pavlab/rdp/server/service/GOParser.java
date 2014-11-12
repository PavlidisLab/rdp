/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubc.pavlab.rdp.server.model.GOTerm;
import ubic.basecode.dataStructure.graph.DirectedGraph;

/**
 * Read in the GO OBO file provided by the Gene Ontology Consortium.
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GOParser {
    private static Log log = LogFactory.getLog( GOParser.class.getName() );
    private Map<String, GOTerm> termMap = new HashMap<String, GOTerm>();
    private DirectedGraph<String, GOTerm> termGraph = new DirectedGraph<String, GOTerm>();

    /**
     * @param i
     * @throws IOException
     */
    public GOParser( InputStream i ) throws IOException {
        if ( i.available() == 0 ) {
            throw new IOException( "Stream contains no data." );
        }

        BufferedReader br = new BufferedReader( new InputStreamReader( i ) );
        GOTerm currentNode = null;
        GOTerm parentNode = null;
        while ( br.ready() ) {
            String line = br.readLine();

            if ( line.equals( "" ) ) {
                // Finished/Between Stanza
                if ( currentNode != null ) {
                    // Just finished Term Stanza
                    // Create Node
                    if ( currentNode.getId().equals( "GO:0042393" ) ) {
                        log.info( currentNode );
                    }
                    termMap.put( currentNode.getId(), currentNode );
                    currentNode = null;
                } else {
                    // Finished unimportant Stanza
                }

            } else if ( line.equals( "[Term]" ) ) {
                // Starting a Term Stanza
                currentNode = new GOTerm();
            } else if ( currentNode != null ) {
                // Within a Term Stanza
                String[] tagValuePair = line.split( ": ", 2 );
                String[] values;
                switch ( tagValuePair[0] ) {
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
                        currentNode.setDefinition( tagValuePair[1] );
                        break;
                    case "is_a":
                        values = tagValuePair[1].split( " " );
                        currentNode.addParent( new GOTerm.Relationship( values[0],
                                GOTerm.Relationship.RelationshipType.IS_A ) );
                        if ( !termMap.containsKey( values[0] ) ) {
                            // parent exists in map
                            parentNode = new GOTerm( values[0] );
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
                                parentNode = new GOTerm( values[1] );
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

    /**
     * Get the graph that was created.
     * 
     * @@return a DirectedGraph. Nodes contain OntologyEntry instances.
     */
    public DirectedGraph<String, GOTerm> getGraph() {
        return termGraph;
    }

    /**
     * Get the graph that was created.
     * 
     * @@return a DirectedGraph. Nodes contain OntologyEntry instances.
     */
    public Map<String, GOTerm> getMap() {
        return termMap;
    }

}