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

package ubc.pavlab.rdp.server.util;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

/**
 * Commonly used JSON helper functions
 * 
 * @author ptan
 * @version $Id$
 */
public class JSONUtil extends gemma.gsec.util.JSONUtil {

    private static Log log = LogFactory.getLog( JSONUtil.class );

    public JSONUtil( HttpServletRequest request, HttpServletResponse response ) {
        super( request, response );
    }

    /**
     * Converts a collection of objects to a json string
     * 
     * @param collection
     * @return
     */
    public String collectionToJson( Collection<?> collection ) {
        StringWriter jsonText = new StringWriter();
        jsonText.write( "[" );

        try {
            Iterator<?> it = collection.iterator();
            while ( it.hasNext() ) {
                Object obj = it.next();
                String delim = it.hasNext() ? "," : "";
                jsonText.append( new JSONObject( obj ).toString() + delim );
            }
            jsonText.append( "]" );
            jsonText.flush();
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText.write( json.toString() );
            log.info( jsonText );
        }

        try {
            jsonText.close();
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText.write( json.toString() );
            log.info( jsonText );
        }

        return jsonText.toString();
    }

}
