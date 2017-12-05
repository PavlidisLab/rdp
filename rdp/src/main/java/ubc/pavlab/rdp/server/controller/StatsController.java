package ubc.pavlab.rdp.server.controller;

import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by mjacobson on 30/11/17.
 */
@Controller
@RemoteProxy
public class StatsController extends BaseController {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    GeneService geneService;

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
            Map<String, Long> countByTaxon = geneService.researcherCountByTaxon();

            JSONObject json = new JSONObject();
            json.put( "success", true );
            json.put( "message", "Statistics succesfully loaded" );
            json.put( "researchers_registered", researchersRegistered );
            json.put( "researchers_registered_with_genes", researchersRegisteredWithGenes );
            json.put( "genes_added", genesAdded );
            json.put( "genes_added_unique", genesAddedUnique );
            json.put( "researcher_counts_by_taxon", new JSONObject( countByTaxon ) );
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
