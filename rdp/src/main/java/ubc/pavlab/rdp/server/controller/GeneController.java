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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GeneCacheService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

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
    protected UserManager userManager;

    @Autowired
    protected ResearcherService researcherService;

    @Autowired
    protected GeneService geneService;

    @Autowired
    protected GeneCacheService geneCacheService;


    /**
     * Force Update NCBI ehcache.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/updateCache.html")
    public void updateCache( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;
        try {
            log.info( "Updating Cache" );
            geneCacheService.clearCache();
            long cacheSize = geneCacheService.updateCache();

            JSONObject json = new JSONObject();
            json.append( "success", true );
            json.append( "message", "Cache size: " + cacheSize );
            jsonText = json.toString();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
            JSONObject json = new JSONObject();
            json.append( "success", false );
            json.append( "message", e.getMessage() );
            jsonText = json.toString();
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    /**
     * Finds exact matching gene symbols (case-insensitive).
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/findGenesByGeneSymbols.html")
    public void findGenesByGeneSymbols( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        final String delimiter = "\n";

        String symbols = request.getParameter( "symbols" );
        Collection<String> querySymbols = new ArrayList<String>(
                Arrays.asList( symbols.toUpperCase().split( delimiter ) ) );
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

        Collection<String> resultSymbols = new ArrayList<>();

        if ( symbols.length() == 0 || querySymbols.size() == 0 ) {
            JSONObject json = new JSONObject();
            json.append( "success", true );
            json.append( "data", resultSymbols );
            json.append( "message", "Please enter a gene symbol" );
            jsonUtil.writeToResponse( json.toString() );
            return;
        }

        try {
            // Collection<Gene> results = biomartService.fetchGenesByGeneSymbols( querySymbols, taxon );
            Collection<Gene> results = geneCacheService.fetchBySymbolsAndTaxon( querySymbols, taxonId );
            for ( Gene gene : results ) {
                resultSymbols.add( gene.getOfficialSymbol().toUpperCase() );
            }

            // symbols not found
            querySymbols.removeAll( resultSymbols );

            JSONObject json = new JSONObject();
            json.append( "success", true );
            json.append( "data", results );
            if ( querySymbols.size() > 0 ) {
                json.append( "message",
                        querySymbols.size() + " symbols not found: " + StringUtils.join( querySymbols, delimiter ) );
            } else {
                json.append( "message", "All " + results.size() + " symbols were found." );
            }
            jsonText = json.toString();
        } catch (Exception e) {
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;
    }

    @RequestMapping("/searchGenes.html")
    public void searchGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        String query = request.getParameter( "query" );

        // Strips non-alphanumeric characters to prevent regex
        query = query.replaceAll( "[^A-Za-z0-9- ]", "" );

        if ( query.length() == 0 ) {
            // Returning illegal json so the select2 fails
            jsonText = "Illegal query";
            jsonUtil.writeToResponse( jsonText );
            return;
        }

        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

        try {
            // Collection<Gene> results = biomartService.findGenes( query, taxon );
            Collection<Gene> results = geneCacheService.fetchByQuery( query, taxonId );
            jsonText = "{\"success\":true,\"data\":" + (new JSONArray( results )).toString() + "}";
        } catch (Exception e) {
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;

    }


}
