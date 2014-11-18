package ubc.pavlab.rdp.server.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import ubc.pavlab.rdp.server.model.Publication;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

@Controller
@RemoteProxy
public class RegisterController extends BaseController {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    UserManager userManager;

    @Autowired
    GeneService geneService;

    @RequestMapping(value = { "", "/", "/register.html", "/home.html", "/index.html" })
    public String showRegister( ModelMap model ) {
        return "register";
    }

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
            Researcher researcher = researcherService.findByUserName( userManager.getCurrentUsername() );
            if ( researcher == null ) {
                researcher = researcherService.create( new Researcher() );
                contact = ( User ) userManager.getCurrentUser();
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

            researcher.setPublications( publications );

            researcherService.update( researcher );
            log.info( "User: (" + userManager.getCurrentUsername() + ") updated profile" );

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Changes saved" );
            jsonText = json.toString();

        } catch ( Exception e ) {
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
            json.put( "data", ( new JSONArray( researchersJson ) ) );
            jsonText = json.toString();
            // jsonText = jsonUtil.collectionToJson( researchers );
        } finally {

            try {
                jsonUtil.writeToResponse( jsonText );
            } catch ( IOException e ) {
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
        String username = userManager.getCurrentUsername();

        /*
         * Object user = researcherService.findByUserName( username ); if ( user == null ) { user =
         * userManager.findByUserName( username ); } else { user = researcherService.thaw( ( Researcher ) user ); }
         */

        Researcher researcher = researcherService.findByUserName( username );

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( researcher == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No researcher with name " + username + "\"}";
            } else {
                // JSONObject json = new JSONObject( user );
                JSONObject json = researcherService.toJSON( researcher );
                log.info( "Loaded Researcher from account: (" + username + "). Account contains "
                        + researcher.getGenes().size() + " Genes." );
                jsonText = "{\"success\":true, \"data\":" + json.toString() + "}";
                // log.debug( "Success! json=" + jsonText );
            }

        } catch ( Exception e ) {
            jsonText = "{\"success\":false,\"message\":\"" + e.getLocalizedMessage() + "\"}";
        } finally {
            jsonUtil.writeToResponse( jsonText );
        }

    }

    /**
     * AJAX entry point. Delete User
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("/deleteUser.html")
    public void deleteUser( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String userName = request.getParameter( "userName" );

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;

        try {
            Researcher r = researcherService.findByUserName( userName );
            researcherService.delete( r );
            log.info( userName + " deleted." );
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "User: (" + userName + ") deleted." );
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

    @RequestMapping("/stats.html")
    public void stats( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        String jsonText = null;
        JSONUtil jsonUtil = null;

        response.setHeader( "Access-Control-Allow-Origin", "*" );
        response.setHeader( "Access-Control-Allow-Methods", "GET, POST, DELETE, PUT" );
        response.setHeader( "Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia" );
        try {
            jsonUtil = new JSONUtil( request, response );

            Long researchersRegistered = researcherService.countResearchers();
            Long researchersRegisteredWithGenes = researcherService.countResearchersWithGenes();
            Long genesAdded = geneService.countAssociations();
            Long genesAddedUnique = geneService.countUniqueAssociations();
            ;
            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Statistics succesfully loaded" );
            json.put( "researchers_registered", researchersRegistered );
            json.put( "researchers_registered_with_genes", researchersRegisteredWithGenes );
            json.put( "genes_added", genesAdded );
            json.put( "genes_added_unique", genesAddedUnique );
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

}