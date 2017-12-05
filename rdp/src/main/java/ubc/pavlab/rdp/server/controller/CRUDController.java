package ubc.pavlab.rdp.server.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.service.GeneAnnotationService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;
import ubc.pavlab.rdp.server.util.Settings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by mjacobson on 05/12/17.
 */
@Controller
@RemoteProxy
public class CRUDController extends BaseController {

    private static Log log = LogFactory.getLog( CRUDController.class );

    @Autowired
    ResearcherService researcherService;

    @Autowired
    protected GeneAnnotationService geneAnnotationService;

    @Autowired
    protected GeneService geneService;


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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
