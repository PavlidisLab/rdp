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

package ubc.pavlab.rdp.server.controller;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ubc.pavlab.rdp.server.biomartquery.BioMartQueryService;
import ubc.pavlab.rdp.server.exception.BioMartServiceException;
import ubc.pavlab.rdp.server.model.GeneValueObject;
import ubc.pavlab.rdp.server.util.JSONUtil;

/**
 * Handles gene-related requests
 * 
 * @author ptan
 * @version $Id$
 */

@Controller
@RemoteProxy
public class GeneController {

    private static Log log = LogFactory.getLog( GeneController.class );

    @Autowired
    protected BioMartQueryService biomartService;

    @RequestMapping("/searchGenes.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        String query = request.getParameter( "query" );

        // FIXME Handle other taxons
        String taxon = request.getParameter( "taxon" );

        try {
            Collection<GeneValueObject> results = biomartService.findGenes( query );
            jsonText = "{\"success\":true,\"data\":" + jsonUtil.collectionToJson( results ) + "}";
        } catch ( BioMartServiceException e ) {
            e.printStackTrace();
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;

    }
}
