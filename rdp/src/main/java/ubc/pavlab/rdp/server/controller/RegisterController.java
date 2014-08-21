package ubc.pavlab.rdp.server.controller;

import gemma.gsec.authentication.UserManager;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

@Controller
@RemoteProxy
public class RegisterController extends BaseController {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    UserManager userManager;

    @RequestMapping(value = { "", "/", "/register.html", "/home.html", "/index.html" })
    public String showRegister( ModelMap model ) {
        return "register";
    }

    /**
     * @RequestMapping("/keep_alive.html") public void loadUser( HttpServletRequest request, HttpServletResponse
     *                                     response ) throws IOException { String jsonText = null; if (
     *                                     !SecurityServiceImpl.isUserLoggedIn() ) { jsonText = "{success:false}"; }
     *                                     else { jsonText = "{success:true}"; } JSONUtil jsonUtil = new JSONUtil(
     *                                     request, response ); jsonUtil.writeToResponse( jsonText ); }
     */

    @RequestMapping("/saveResearcher.html")
    public void saveResearcher( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String firstName = request.getParameter( "firstName" );
        String lastName = request.getParameter( "lastName" );
        String organization = request.getParameter( "organization" );
        String department = request.getParameter( "department" );
        String email = request.getParameter( "email" );
        String website = request.getParameter( "website" );
        String phone = request.getParameter( "phone" );

        try {
            User contact = null;
            Researcher researcher = researcherService.findByEmail( email );
            if ( researcher == null ) {
                researcher = researcherService.create( new Researcher() );
                contact = ( User ) userManager.findbyEmail( email );
                researcher.setContact( contact );
            } else {
                contact = researcher.getContact();
            }
            if ( contact != null ) {
                contact.setEmail( email );
                contact.setFirstName( firstName );
                contact.setLastName( lastName );
            }
            researcher.setDepartment( department );
            researcher.setOrganization( organization );
            researcher.setPhone( phone );
            researcher.setWebsite( website );

            researcherService.update( researcher );

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

    @RequestMapping("/deleteResearcherGene.html")
    public void deleteResearcherGene( HttpServletRequest request, HttpServletResponse response ) throws IOException {
        // FIXME
    }

    @RequestMapping("/saveResearcherGene.html")
    public void saveResearcherGene( HttpServletRequest request, HttpServletResponse response ) throws IOException {

        // FIXME

        String jsonText = null;
        JSONUtil jsonUtil = new JSONUtil( request, response );

        String userName = request.getParameter( "userName" );
        String geneSymbol = request.getParameter( "geneSymbol" );
        String taxonCommonName = request.getParameter( "taxonCommonName" );

        try {
            Researcher researcher = researcherService.findByEmail( userName );

            // FIXME Lookup Gene
            Gene gene = new Gene( geneSymbol );
            Taxon taxon = new Taxon();
            taxon.setCommonName( taxonCommonName );
            gene.setTaxon( taxon );
            researcher.getGenes().add( gene );
            researcherService.update( researcher );

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

    /**
     * AJAX entry point. Loads all Researchers.
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadAllResearchers.html")
    /*
     * public void loadAllResearchers( HttpServletRequest request, HttpServletResponse response ) {
     * 
     * StringWriter jsonText = new StringWriter(); jsonText.write( "[" );
     * 
     * JSONUtil jsonUtil = new JSONUtil( request, response );
     * 
     * try { java.util.Iterator<Researcher> it = researcherService.loadAll().iterator(); while ( it.hasNext() ) {
     * Researcher user = it.next(); String delim = it.hasNext() ? "," : ""; jsonText.append( new JSONObject( user
     * ).toString() + delim ); } jsonText.append( "]" ); jsonText.flush(); } catch ( Exception e ) { log.error(
     * e.getLocalizedMessage(), e ); JSONObject json = new JSONObject(); json.put( "success", false ); json.put(
     * "message", e.getLocalizedMessage() ); jsonText.write( json.toString() ); log.info( jsonText ); } finally { String
     * str = jsonText.toString(); try { jsonUtil.writeToResponse( str ); jsonText.close(); } catch ( IOException e ) {
     * log.error( e.getLocalizedMessage(), e ); JSONObject json = new JSONObject(); json.put( "success", false );
     * json.put( "message", e.getLocalizedMessage() ); jsonText.write( json.toString() ); log.info( jsonText ); } } }
     */
    public void loadAllResearchers( HttpServletRequest request, HttpServletResponse response ) {

        String jsonText = "";
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            jsonText = jsonUtil.collectionToJson( researcherService.loadAll() );
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication.isAuthenticated();

        if ( !isAuthenticated ) {
            log.error( "Researcher not authenticated.  Cannot populate researcher data." );
            return;
        }

        Object o = authentication.getPrincipal();
        String username = null;

        if ( o instanceof UserDetails ) {
            username = ( ( UserDetails ) o ).getUsername();
        } else {
            username = o.toString();
        }

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
                // jsonText = "{\"success\":true, \"data\":{\"username\":" + "\"" + username + "\"" + ",\"email\":" +
                // "\""
                // + user.getEmail() + "\"" + "}}";

                JSONObject json = new JSONObject( user );
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
     * Returns the list of genes for the given Researcher and Model Organism.
     * 
     * @param request
     * @param response
     */
    @RequestMapping("/loadResearcherGenes.html")
    public void loadResearcherGenes( HttpServletRequest request, HttpServletResponse response ) {
        JSONUtil jsonUtil = new JSONUtil( request, response );
        String jsonText = "";

        String username = request.getParameter( "userName" );
        String taxonCommonName = request.getParameter( "taxonCommonName" );

        Researcher user = researcherService.findByUserName( username );

        try {

            if ( user == null ) {

                // this shouldn't happen.
                jsonText = "{\"success\":false,\"message\":\"No researcher with name " + username + "\"}";
            } else {

                // FIXME Filter by organism
                jsonText = "{\"success\":true,\"data\":" + jsonUtil.collectionToJson( user.getGenes() ) + "}";
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
}