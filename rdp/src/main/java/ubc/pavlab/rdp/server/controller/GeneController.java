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

import gemma.gsec.authentication.UserManager;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ubc.pavlab.rdp.server.biomartquery.BioMartQueryService;
import ubc.pavlab.rdp.server.exception.BioMartServiceException;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneValueObject;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
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
    protected UserManager userManager;

    @Autowired
    protected ResearcherService researcherService;

    @Autowired
    protected GeneService geneService;

    @Autowired
    protected BioMartQueryService biomartService;

    /**
     * Returns the list of genes for the given Researcher and Model Organism.
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadResearcherGenes.html")
    public void loadResearcherGenes( HttpServletRequest request, HttpServletResponse response ) {
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = "";

        String username = userManager.getCurrentUsername();
        final String taxonCommonName = request.getParameter( "taxonCommonName" );

        Researcher user = researcherService.findByUserName( username );

        try {

            if ( user == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No researcher with name " + username + "\"}";
            } else {

            	Collection<Gene> genes = user.getGenes();
            	CollectionUtils.filter( genes, new Predicate(){
					public boolean evaluate( Object input ) {
                       return ((Gene) input).getTaxon().equals(taxonCommonName);
                    }
                 } );
                jsonText = "{\"success\":true,\"data\":" + jsonUtil.collectionToJson( genes ) + "}";
                log.info( "Loaded " + genes.size() + " genes" );
            }

        } catch ( Exception e ) {
            jsonText = "{\"success\":false,\"message\":\"" + e.getLocalizedMessage() + "\"}";
        } finally {

            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a collection of Gene objects from the json
     * 
     * @param genesJSON - a JSON representation of an array of Genes
     * @return
     */
    private Collection<Gene> deserealizeGenes( String genesJSON ) {
        Collection<Gene> results = new HashSet<>();
        JSONObject json = new JSONObject( genesJSON );

        for ( Object key : json.keySet() ) {
            String symbol = ( String ) key;

            // try looking for an existing one
            Collection<Gene> genesFound = geneService.findByOfficalSymbol( symbol );
            if ( genesFound.size() > 0 ) {
                results.addAll( genesFound );
            } else {

                // it doesn't exist yet
                // biomartService.fetchGenesByGeneSymbols( geneSymbols )
                Gene gene = new Gene();
                gene.parseJSON( json.get( symbol ).toString() );
                results.add( geneService.create( gene ) );
            }
        }
        return results;
    }

    @RequestMapping("/saveResearcherGenes.html")
    public void saveResearcherGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        // FIXME

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String username = userManager.getCurrentUsername();
        String genesJSON = request.getParameter( "genes" ); // {"ensemblId":"ENSG00000105393","officialSymbol":"BABAM1","officialName":"BRISC and BRCA1 A complex member 1","label":"BABAM1","geneBioType":"protein_coding","key":"BABAM1:human","taxon":"human","genomicRange":{"baseStart":17378159,"baseEnd":17392058,"label":"19:17378159-17392058","htmlLabel":"19:17378159-17392058","bin":65910,"chromosome":"19","tooltip":"19:17378159-17392058"},"text":"<b>BABAM1</b> BRISC and BRCA1 A complex member 1"}
        String taxonCommonName = request.getParameter( "taxonCommonName" );

        try {
            Researcher researcher = researcherService.findByUserName( username );

            Collection<Gene> genes = new HashSet<>();
            genes.addAll( deserealizeGenes( genesJSON ) );
            researcherService.updateGenes( researcher, genes );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Changes saved" );
            jsonText = json.toString();

        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", e.getLocalizedMessage() );
            jsonText = json.toString();
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

    @RequestMapping("/searchGenes.html")
    public void searchGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = null;

        String query = request.getParameter( "query" );

        // FIXME Handle other taxons
        String taxon = request.getParameter( "taxon" );

        try {
            Collection<GeneValueObject> results = biomartService.findGenes( query, taxon );
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
