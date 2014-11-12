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

package ubc.pavlab.rdp.server.security.service;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubc.pavlab.rdp.server.model.GOTerm;
import ubc.pavlab.rdp.server.service.GOParser;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */

public class GOParserTest {

    private static Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );

    // note: no spring context.
    @Test
    public void testTest() throws Exception {
        log.info( "test log message" );
        InputStream z = GOParserTest.class.getResourceAsStream( "/data/go.obo" );
        GOParser gOParser = new GOParser( z );
        Map<String, GOTerm> termMap = gOParser.getMap();
        log.info( termMap.size() );
        log.info( termMap.get( "GO:0000003" ) );
        log.info( termMap.get( "GO:0000008" ) );
        log.info( termMap.get( "GO:0042393" ) );
        log.info( termMap.get( "GO:0042393" ).getChildren() );
        log.info( termMap.get( "GO:0042393" ).getParents() );

        log.info( termMap.get( "GO:0000022" ) );
        log.info( termMap.get( "GO:0000022" ).getChildren() );
        log.info( termMap.get( "GO:0000022" ).getParents() );
    }
}
