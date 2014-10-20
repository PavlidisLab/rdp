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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import ubc.pavlab.rdp.server.model.GeneAssociation;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GeneAnnotationService;
import ubc.pavlab.rdp.server.service.GeneCacheService;
import ubc.pavlab.rdp.server.service.GeneOntologyService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.service.TaxonService;
import ubc.pavlab.rdp.server.util.JSONUtil;
import ubc.pavlab.rdp.server.util.Settings;
import ubic.basecode.ontology.model.OntologyTerm;

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
    protected GeneAnnotationService geneAnnotationService;

    @Autowired
    protected GeneOntologyService geneOntologyService;

    @Autowired
    protected TaxonService taxonService;

    /*
     * @Autowired protected BioMartQueryService biomartService;
     */

    @Autowired
    protected GeneCacheService geneCacheService;

    /**
     * AJAX entry point. Loads the Researcher who's currently logged in.
     * 
     * @param request
     * @param response
     */
    @Deprecated
    @RequestMapping("/findResearchersByGene.html")
    public void findResearchersByGene( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String[] genesJSON = request.getParameterValues( "gene[]" );
        String taxonCommonName = request.getParameter( "taxonCommonName" );

        Collection<Researcher> researchers = new HashSet<>();

        try {

            JSONObject json = new JSONObject();

            for ( Entry<Gene, TierType> entry : geneService.deserializeGenes( genesJSON ).entrySet() ) {
                Gene gene = entry.getKey();
                TierType tier = entry.getValue();
                researchers.addAll( researcherService.findByGene( gene ) );
            }

            Set<String> researchersJson = new HashSet<String>();
            for ( Researcher r : researchers ) {
                researchersJson.add( researcherService.toJSON( r ).toString() );
            }

            JSONArray array = new JSONArray( researchersJson );

            json.put( "data", array.toString() );
            json.put( "success", true );
            json.put( "message", "Found " + researchers.size() + " researchers" );
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

    /*
     * Used to save genes selected by researcher in the front-end table
     */
    @RequestMapping("/saveResearcherGenes.html")
    public void saveResearcherGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String username = userManager.getCurrentUsername();
        // String taxonCommonName = request.getParameter( "taxonCommonName" );
        String[] genesJSON = request.getParameterValues( "genes[]" );
        String taxonDescriptions = request.getParameter( "taxonDescriptions" );

        if ( genesJSON == null ) {
            log.info( username + ": No genes to save" );
            genesJSON = new String[] {};
        }

        try {
            Researcher researcher = researcherService.findByUserName( username );

            // Update Organism Descriptions
            JSONObject jsonDescriptionSet = new JSONObject( taxonDescriptions );

            for ( Object key : jsonDescriptionSet.keySet() ) {
                String taxon = ( String ) key;
                String td = jsonDescriptionSet.get( taxon ).toString();
                researcher.updateTaxonDescription( Long.parseLong( taxon, 10 ), td );
            }

            // Update Genes
            researcherService.updateGenes( researcher, geneService.deserializeGenes( genesJSON ) ); // This updates the
                                                                                                    // researcher
                                                                                                    // persistence for
                                                                                                    // both

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

    @RequestMapping("/loadGenes.html")
    public void loadGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {
            String username = userManager.getCurrentUsername();

            Researcher researcher = researcherService.findByUserName( username );

            Collection<GeneAssociation> geneAssociations = researcher.getGeneAssociations();

            JSONArray jsonArray = geneService.toJSON( geneAssociations );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Genes Loaded" );
            json.put( "genes", jsonArray );
            json.put( "size", jsonArray.length() );
            jsonText = json.toString();
            log.info( jsonText );
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
        } catch ( Exception e ) {
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
        } catch ( Exception e ) {
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
        query = query.replaceAll( "[^A-Za-z0-9]", "" );

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
            jsonText = "{\"success\":true,\"data\":" + ( new JSONArray( results ) ).toString() + "}";
        } catch ( Exception e ) {
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;

    }

    @RequestMapping("/resetGeneTable.html")
    public void resetGeneTable( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        String filePath = Settings.getString( "rdp.ftp.genePath" );
        log.info( "Resetting GENE table from path: " + filePath );
        try {
            geneService.truncateGeneTable();
            log.info( "GENE table truncated" );
            geneService.updateGeneTable( filePath );
            log.info( "GENE table updated." );
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "GENE table reset." );
            jsonText = json.toString();
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", "An error occurred!" );
            json.put( "error", e.getLocalizedMessage() );
            jsonText = json.toString();
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    @RequestMapping("/resetGeneAnnotationTable.html")
    public void resetGeneAnnotationTable( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        String filePath = Settings.getString( "rdp.ftp.geneAnnotationPath" );
        log.info( "Resetting GENE_ANNOTATION table from path: " + filePath );
        try {
            geneAnnotationService.truncateGeneAnnotationTable();
            log.info( "GENE_ANNOTATION table truncated" );
            geneAnnotationService.updateGeneAnnotationTable( filePath );
            log.info( "GENE_ANNOTATION table updated." );
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "GENE_ANNOTATION table reset." );
            jsonText = json.toString();
        } catch ( Exception e ) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", "An error occurred!" );
            json.put( "error", e.getLocalizedMessage() );
            jsonText = json.toString();
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }
    }

    @RequestMapping("/getRelatedTerms.html")
    public void getRelatedTerms( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {
            String username = userManager.getCurrentUsername();

            Researcher researcher = researcherService.findByUserName( username );

            Collection<Gene> genes = researcher.getGenes();
            String minimumFrequency = request.getParameter( "minimumFrequency" );
            String minimumTermSize = request.getParameter( "minimumTermSize" );
            String maximumTermSize = request.getParameter( "maximumTermSize" );
            Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

            int minFreq = 2;
            if ( minimumFrequency != null ) {
                minFreq = Integer.parseInt( minimumFrequency );
            }

            int maxTerm = 100;
            if ( maximumTermSize != null ) {
                maxTerm = Integer.parseInt( maximumTermSize );
            }

            int minTerm = 10;
            if ( minimumTermSize != null ) {
                minTerm = Integer.parseInt( minimumTermSize );
            }

            Map<String, Map<OntologyTerm, Long>> goTermsResult = geneOntologyService.calculateGoTermFrequency( genes,
                    taxonId, minFreq, minTerm, maxTerm );
            List<GeneOntologyTerm> goTerms = new ArrayList<GeneOntologyTerm>();
            for ( OntologyTerm term : goTermsResult.get( "frequency" ).keySet() ) {
                GeneOntologyTerm goTerm = new GeneOntologyTerm( term );
                goTerm.setFrequency( goTermsResult.get( "frequency" ).get( term ) );
                goTerm.setSize( goTermsResult.get( "size" ).get( term ) );
                goTerms.add( goTerm );
            }

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", goTerms.size() + " Terms Loaded" );
            json.put( "terms", goTerms );
            jsonText = json.toString();
            log.info( jsonText );
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
}
