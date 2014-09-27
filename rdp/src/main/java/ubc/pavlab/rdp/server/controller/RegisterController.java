package ubc.pavlab.rdp.server.controller;

import gemma.gsec.authentication.UserManager;

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

import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

@Controller
@RemoteProxy
public class RegisterController extends BaseController {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    GeneService geneService;

    @Autowired
    UserManager userManager;

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
            Set<String> researchersJson = new HashSet<String>();
            for ( Researcher r : researchers ) {
                researchersJson.add( r.toJSON().toString() );
            }

            jsonText = ( new JSONArray( researchersJson ) ).toString();
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
     */
    @RequestMapping("/loadResearcher.html")
    public void loadUser( HttpServletRequest request, HttpServletResponse response ) {
        String username = userManager.getCurrentUsername();

        Object user = researcherService.findByUserName( username );
        if ( user == null ) {
            user = userManager.findByUserName( username );
        } else {
            user = researcherService.thaw( ( Researcher ) user );
        }

        JSONUtil jsonUtil = new JSONUtil( request, response );

        String jsonText = null;
        try {

            if ( user == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No researcher with name " + username + "\"}";
            } else {
                // JSONObject json = new JSONObject( user );
                JSONObject json = ( ( Researcher ) user ).toJSON();
                log.info( "Loaded Researcher from account: (" + username + "). Account contains "
                        + ( ( Researcher ) user ).getGenes().size() + " Genes." );
                jsonText = "{\"success\":true, \"data\":" + json.toString() + "}";
                // log.debug( "Success! json=" + jsonText );
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

}