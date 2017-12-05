package ubc.pavlab.rdp.server.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.model.GOTerm;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GOService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by mjacobson on 05/12/17.
 */
@Controller
@RemoteProxy
public class TermController {

    private static Log log = LogFactory.getLog( TermController.class );

    @Autowired
    protected UserManager userManager;

    @Autowired
    ResearcherService researcherService;

    @Autowired
    GOService gOService;


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
        } catch (Exception e) {
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
            } catch (IndexOutOfBoundsException e) {
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

            jsonText = "{\"success\":true,\"data\":" + (gOService.toJSON( goTerms )).toString() + "}";
        } catch (Exception e) {
            log.error( e.getMessage(), e );
            jsonText = "{\"success\":false,\"message\":" + e.getMessage() + "\"}";
        }

        jsonUtil.writeToResponse( jsonText );
        return;
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

        } catch (Exception e) {
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

        } catch (Exception e) {
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
