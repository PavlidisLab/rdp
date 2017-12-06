package ubc.pavlab.rdp.server.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.model.*;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GOService;
import ubc.pavlab.rdp.server.service.GeneService;
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
public class ResearcherController {

    private static Log log = LogFactory.getLog( ResearcherController.class );

    private static long GO_SIZE_LIMIT = 100;

    @Autowired
    protected UserManager userManager;

    @Autowired
    ResearcherService researcherService;

    @Autowired
    protected GeneService geneService;

    @Autowired
    GOService gOService;

    @RequestMapping("/saveResearcher.html")
    public void saveResearcher( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String firstName = request.getParameter( "firstName" );
        String lastName = request.getParameter( "lastName" );
        String organization = request.getParameter( "organization" );
        String department = request.getParameter( "department" );
        // String email = request.getParameter( "email" );
        String website = request.getParameter( "website" );
        String phone = request.getParameter( "phone" );
        String description = request.getParameter( "description" );
        String[] pubMedIdsJSON = request.getParameterValues( "pubMedIds[]" );

        Set<Publication> publications = new HashSet<Publication>();
        if ( pubMedIdsJSON != null ) {
            for ( int i = 0; i < pubMedIdsJSON.length; i++ ) {
                publications.add( new Publication( Integer.valueOf( pubMedIdsJSON[i] ) ) );
            }
        }

        try {
            User contact = null;
            Researcher researcher = researcherService.loadCurrentResearcher();
            if ( researcher == null ) {
                researcher = researcherService.create( new Researcher() );
                contact = (User) userManager.getCurrentUser();
                researcher.setContact( contact );
            } else {
                contact = researcher.getContact();
            }
            if ( contact != null ) {
                // contact.setEmail( email );
                contact.setFirstName( firstName );
                contact.setLastName( lastName );
            }
            researcher.setDepartment( department );
            researcher.setOrganization( organization );
            researcher.setPhone( phone );
            researcher.setWebsite( website );
            researcher.setDescription( description );

            researcherService.updatePublications( researcher, publications );

            researcherService.update( researcher );
            log.info( "User: (" + userManager.getCurrentUsername() + ") updated profile" );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Changes saved" );
            jsonText = json.toString();

        } catch (Exception e) {
            log.error( e.getLocalizedMessage(), e );
            JSONObject json = new JSONObject();
            json.put( "success", false );
            json.put( "message", "An error occurred, could not save changes!" );
            json.put( "error", e.getLocalizedMessage() );
            jsonText = json.toString();
            log.info( jsonText );
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

    /**
     * AJAX entry point. Loads all Researchers.
     *
     * @param request
     * @param response
     */
    @RequestMapping("/loadAllResearchers.html")
    public void loadAllResearchers( HttpServletRequest request, HttpServletResponse response ) {

        String jsonText = "";
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            Collection<Researcher> researchers = researcherService.loadAll();
            Set<JSONObject> researchersJson = new HashSet<JSONObject>();
            for ( Researcher r : researchers ) {
                researchersJson.add( researcherService.toJSON( r ) );
            }
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "All researchers loaded." );
            json.put( "data", (new JSONArray( researchersJson )) );
            jsonText = json.toString();
            // jsonText = jsonUtil.collectionToJson( researchers );
        } finally {

            try {
                jsonUtil.writeToResponse( jsonText );
            } catch (IOException e) {
                log.error( e.getLocalizedMessage(), e );
                JSONObject json = new JSONObject();
                json.put( "success", false );
                json.put( "message", e.getLocalizedMessage() );
                jsonText = json.toString();
                log.info( jsonText );
            }
        }
    }

    /**
     * AJAX entry point. Loads the Researcher who's currently logged in.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/loadResearcher.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        Researcher researcher = researcherService.loadCurrentResearcher();

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( researcher == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No researcher found\"}";
            } else {
                // JSONObject json = new JSONObject( user );
                JSONObject json = researcherService.toJSON( researcher );
                log.info( "Loaded Researcher from account: (" + researcher.getContact().getUserName() + "). Account contains "
                        + researcher.getGenes().size() + " Genes." );
                jsonText = "{\"success\":true, \"data\":" + json.toString() + "}";
                // log.debug( "Success! json=" + jsonText );
            }

        } catch (Exception e) {
            jsonText = "{\"success\":false,\"message\":\"" + e.getLocalizedMessage() + "\"}";
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

        // Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        String[] genesJSON = request.getParameterValues( "genes[]" );
        String taxonDescriptions = request.getParameter( "taxonDescriptions" );

        if ( genesJSON == null ) {
            log.info( "No genes to save" );
            genesJSON = new String[]{};
        }

        try {
            Researcher researcher = researcherService.loadCurrentResearcher();

            // Update Organism Descriptions
            JSONObject jsonDescriptionSet = new JSONObject( taxonDescriptions );

            for ( Object key : jsonDescriptionSet.keySet() ) {
                String taxon = (String) key;
                String td = jsonDescriptionSet.get( taxon ).toString();
                researcher.updateTaxonDescription( Long.parseLong( taxon, 10 ), td );
            }

            // Update Genes
            researcherService.updateGenes( researcher, geneService.quickDeserializeGenes( genesJSON ) );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Changes saved" );
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

    /*
 * Used to save genes selected by researcher in the front-end table
 */
    @RequestMapping("/saveGenesByTaxon.html")
    public void saveGenesByTaxon( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        String[] genesJSON = request.getParameterValues( "genes[]" );
        String taxonDescription = request.getParameter( "taxonDescription" );

        if ( genesJSON == null ) {
            log.info( "No genes to save" );
            genesJSON = new String[]{};
        }

        try {
            Researcher researcher = researcherService.loadCurrentResearcher();

            // Update Organism Description
            researcher.updateTaxonDescription( taxonId, taxonDescription.trim() );

            // remove genes not in specified taxon
            HashMap<Gene, GeneAssociation.TierType> genes = geneService.quickDeserializeGenes( genesJSON );
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

    @Deprecated
    @RequestMapping("/loadGenes.html")
    public void loadGenes( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {
            Researcher researcher = researcherService.loadCurrentResearcher();

            Collection<GeneAssociation> geneAssociations = researcher.getGeneAssociations();

            JSONArray jsonArray = geneService.toJSON( geneAssociations );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Genes Loaded" );
            json.put( "genes", jsonArray );
            json.put( "size", jsonArray.length() );
            jsonText = json.toString();
            log.info( jsonText );
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
 * Used to save GO Terms selected by researcher in the front-end
 */
    @RequestMapping("/saveResearcherGOTerms.html")
    public void saveResearcherGOTerms( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );
        String[] GOJSON = request.getParameterValues( "terms[]" );
        String taxonDescription = request.getParameter( "taxonDescription" );

        if ( GOJSON == null ) {
            log.info( "No terms to save" );
            GOJSON = new String[]{};
        }

        try {
            Researcher researcher = researcherService.loadCurrentResearcher();

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
