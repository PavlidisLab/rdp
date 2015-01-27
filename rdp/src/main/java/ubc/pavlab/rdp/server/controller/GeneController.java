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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

import ubc.pavlab.rdp.server.model.GOTerm;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GOService;
import ubc.pavlab.rdp.server.service.GeneAnnotationService;
import ubc.pavlab.rdp.server.service.GeneCacheService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.service.TaxonService;
import ubc.pavlab.rdp.server.util.JSONUtil;
import ubc.pavlab.rdp.server.util.Settings;

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

    private static long GO_SIZE_LIMIT = 100;

    @Autowired
    protected UserManager userManager;

    @Autowired
    protected ResearcherService researcherService;

    @Autowired
    protected GeneService geneService;

    @Autowired
    protected GeneAnnotationService geneAnnotationService;

    // @Autowired
    // protected GeneOntologyService geneOntologyService;

    @Autowired
    GOService gOService;

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
        // Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
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
            researcherService.updateGenes( researcher, geneService.quickDeserializeGenes( genesJSON ) );

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

    /*
     * Used to save genes selected by researcher in the front-end table
     */
    @RequestMapping("/saveGenesByTaxon.html")
    public void saveGenesByTaxon( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String username = userManager.getCurrentUsername();
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        String[] genesJSON = request.getParameterValues( "genes[]" );
        String taxonDescription = request.getParameter( "taxonDescription" );

        if ( genesJSON == null ) {
            log.info( username + ": No genes to save" );
            genesJSON = new String[] {};
        }

        try {
            Researcher researcher = researcherService.findByUserName( username );

            // Update Organism Description
            researcher.updateTaxonDescription( taxonId, taxonDescription );

            // remove genes not in specified taxon
            HashMap<Gene, TierType> genes = geneService.quickDeserializeGenes( genesJSON );
            for ( Iterator<Gene> i = genes.keySet().iterator(); i.hasNext(); ) {
                Gene gene = i.next();
                if ( !gene.getTaxonId().equals( taxonId ) ) {
                    i.remove();
                }
            }

            // Update Genes
            researcherService.updateGenesByTaxon( researcher, taxonId, genes );
            researcherService.calculateGenes( researcher, taxonId );
            researcherService.refreshOverlaps( researcher, taxonId );
            // researcherService.updateGenes( researcher, geneService.quickDeserializeGenes( genesJSON ) );

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

            String minimumFrequency = request.getParameter( "minimumFrequency" );
            String minimumTermSize = request.getParameter( "minimumTermSize" );
            String maximumTermSize = request.getParameter( "maximumTermSize" );
            Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

            Collection<Gene> genes = researcher.getDirectGenesInTaxon( taxonId );

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

            log.debug( "Loading GO Terms for " + username );
            Map<GOTerm, Long> goTermsResult = gOService.calculateGoTermFrequency( genes, taxonId, minFreq, minTerm,
                    maxTerm );
            List<GeneOntologyTerm> goTerms = new ArrayList<GeneOntologyTerm>();
            for ( GOTerm term : goTermsResult.keySet() ) {
                GeneOntologyTerm goTerm = new GeneOntologyTerm( term );
                goTerm.setFrequency( goTermsResult.get( term ) );
                goTerm.setSize( gOService.getGeneSizeInTaxon( goTerm.getGeneOntologyId(), taxonId ) );
                goTerms.add( goTerm );
            }

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", goTerms.size() + " Terms Loaded" );
            json.put( "terms", gOService.toJSON( goTerms ) );
            jsonText = json.toString();
            // log.info( jsonText );
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

    @RequestMapping("/searchGO.html")
    public void searchGO( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        String query = request.getParameter( "query" );
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

        // Strips non-alphanumeric characters to prevent regex
        // query = query.replaceAll( "[^A-Za-z0-9]", "" );

        if ( query.length() == 0 ) {
            // Returning illegal json so the select2 fails
            jsonText = "Illegal query";
            jsonUtil.writeToResponse( jsonText );
            return;
        }

        try {

            // List<GeneOntologyTerm> results = new ArrayList<GeneOntologyTerm>( gOService.fetchByQuery( query ) );
            List<GOTerm> results = new ArrayList<GOTerm>( gOService.search( query ) );

            // Only return max 100 hits
            try {
                results = results.subList( 0, 100 );
            } catch ( IndexOutOfBoundsException e ) {
                // ignore
            }

            List<GeneOntologyTerm> goTerms = new ArrayList<GeneOntologyTerm>();
            for ( GOTerm term : results ) {
                GeneOntologyTerm goTerm = new GeneOntologyTerm( term );
                // term.setFrequency( 0L );
                goTerm.setSize( gOService.getGeneSizeInTaxon( term, taxonId ) );
                goTerms.add( goTerm );
            }

            /*
             * for ( Iterator<GeneOntologyTerm> i = results.iterator(); i.hasNext(); ) { GeneOntologyTerm term =
             * i.next(); if ( term.getSize() > 100L ) { i.remove(); } }
             */

            jsonText = "{\"success\":true,\"data\":" + ( gOService.toJSON( goTerms ) ).toString() + "}";
        } catch ( Exception e ) {
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;
    }

    /*
     * Used to save GO Terms selected by researcher in the front-end
     */
    @RequestMapping("/saveResearcherGOTerms.html")
    public void saveResearcherGOTerms( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String username = userManager.getCurrentUsername();
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        String[] GOJSON = request.getParameterValues( "terms[]" );
        String taxonDescription = request.getParameter( "taxonDescription" );

        if ( GOJSON == null ) {
            log.info( username + ": No terms to save" );
            GOJSON = new String[] {};
        }

        try {
            Researcher researcher = researcherService.findByUserName( username );

            // Update Organism Description
            researcher.updateTaxonDescription( taxonId, taxonDescription );

            // Deserialize GO Terms
            // Collection<GeneOntologyTerm> goTermsInMemory = gOService.deserializeGOTerms( GOJSON );
            Collection<GOTerm> goTermsInMemory = gOService.deserializeGOTerms( GOJSON );

            Collection<Gene> genes = researcher.getDirectGenesInTaxon( taxonId );

            // Add taxonId to terms and find sizes
            Collection<GeneOntologyTerm> goTermsToBePersisted = new HashSet<GeneOntologyTerm>();
            for ( GOTerm term : goTermsInMemory ) {
                // Necessary to save a new instance as the one in memory cannot be changed without Hibernate throwing
                // stale state exceptions on updates
                GeneOntologyTerm newTerm = new GeneOntologyTerm( term );
                newTerm.setTaxonId( taxonId );
                newTerm.setSize( gOService.getGeneSizeInTaxon( term.getId(), taxonId ) );
                newTerm.setFrequency( gOService.computeOverlapFrequency( term.getId(), genes ) );
                if ( newTerm.getSize() < GO_SIZE_LIMIT ) {
                    goTermsToBePersisted.add( newTerm );
                }
            }

            // Update GO Terms for this taxon
            researcherService.updateGOTermsForTaxon( researcher, goTermsToBePersisted, taxonId );
            researcherService.calculateGenes( researcher, taxonId );
            // HashMap<Gene, TierType> calculatedGenes = new HashMap<Gene, TierType>();
            //
            // for ( Gene g : gOService.getRelatedGenes( goTermsToBePersisted, taxonId ) ) {
            // calculatedGenes.put( g, TierType.TIER3 );
            // }
            //
            // Collection<TierType> tiersToRemove = new HashSet<TierType>();
            // tiersToRemove.add( TierType.TIER3 );
            // researcherService.removeGenesByTiersAndTaxon( researcher, tiersToRemove, taxonId );
            // researcherService.addGenes( researcher, calculatedGenes );

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

    /*
     * Used to calculate GO Term size on the fly
     */
    @RequestMapping("/getGOTermStats.html")
    public void getGOTermStats( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String geneOntologyId = request.getParameter( "geneOntologyId" );
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        try {
            String username = userManager.getCurrentUsername();

            Researcher researcher = researcherService.findByUserName( username );
            // Deserialize GO Terms
            Long size = gOService.getGeneSizeInTaxon( geneOntologyId, taxonId );

            Collection<Gene> genes = researcher.getDirectGenesInTaxon( taxonId );
            Long frequency = gOService.computeOverlapFrequency( geneOntologyId, genes );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Stats calculated" );
            json.put( "geneSize", size );
            json.put( "frequency", frequency );
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
     * Used to calculate list of GO Term size on the fly
     */
    @RequestMapping("/getGOTermsStats.html")
    public void getGOTermsStats( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        Collection<String> geneOntologyIds = new HashSet<String>( Arrays.asList( request
                .getParameterValues( "geneOntologyIds[]" ) ) );
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        try {
            String username = userManager.getCurrentUsername();

            Researcher researcher = researcherService.findByUserName( username );
            // Deserialize GO Terms
            JSONObject results = new JSONObject();
            for ( String id : geneOntologyIds ) {
                Long size = gOService.getGeneSizeInTaxon( id, taxonId );

                Collection<Gene> genes = researcher.getDirectGenesInTaxon( taxonId );
                Long frequency = gOService.computeOverlapFrequency( id, genes );
                JSONObject json = new JSONObject();
                json.put( "geneSize", size );
                json.put( "frequency", frequency );
                results.put( id, json );
            }

            results.put( "success", true );
            results.put( "message", "Stats calculated" );
            jsonText = results.toString();

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
     * Used to get genes of a specific taxon in gene pool of a GO TERM
     */
    @RequestMapping("/getGenePool.html")
    public void getGenePool( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String geneOntologyId = request.getParameter( "geneOntologyId" );
        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        try {

            Collection<Gene> genes = gOService.getGenes( geneOntologyId, taxonId );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Gene Pool for " + geneOntologyId + " in " + taxonId );
            json.put( "genePool", genes );
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

}
