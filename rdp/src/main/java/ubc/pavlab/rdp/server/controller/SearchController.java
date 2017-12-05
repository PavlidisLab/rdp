package ubc.pavlab.rdp.server.controller;

import org.apache.commons.lang3.StringUtils;
import org.directwebremoting.annotations.RemoteProxy;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.util.JSONUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 30/11/17.
 */
@Controller
@RemoteProxy
public class SearchController extends BaseController {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    GeneService geneService;

    /**
     * AJAX entry point. Find Researchers based on like search parameters.
     *
     * @param request
     * @param response
     */
    @RequestMapping("/searchResearchersByName.html")
    public void searchResearchersByName( HttpServletRequest request, HttpServletResponse response ) {

        String jsonText = "";
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            String nameLike = request.getParameter( "nameLike" );

            if ( StringUtils.isBlank( nameLike ) ) {
                JSONObject json = new JSONObject();
                json.put( "success", false );
                json.put( "message", "Problem with query" );
                jsonText = json.toString();
                log.info( jsonText );
            } else {

                Collection<Researcher> researchers = researcherService.findByLikeName( nameLike );

                Set<JSONObject> researchersJson = new HashSet<JSONObject>();
                for ( Researcher r : researchers ) {
                    researchersJson.add( researcherService.toJSON( r ) );
                }
                JSONObject json = new JSONObject();
                json.put( "success", true );
                json.put( "message", "Found " + researchers.size() + " researchers." );
                json.put( "data", (new JSONArray( researchersJson )) );
                jsonText = json.toString();
            }
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
     * AJAX entry point. Find Researchers based on like search parameters.
     *
     * @param request
     * @param response
     */
    @RequestMapping("/searchResearchersLike.html")
    public void searchResearchersLike( HttpServletRequest request, HttpServletResponse response ) {

        String jsonText = "";
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            String tierSelection = request.getParameter( "tier" );
            String likeSymbol = request.getParameter( "likeSymbol" );
            log.info( tierSelection );
            log.info( likeSymbol );
            log.info( request.getParameter( "taxonId" ) );
            TierType tier = StringUtils.isBlank( tierSelection ) ? null : TierType.valueOf( tierSelection );
            Long taxonId = Long.parseLong( request.getParameter( "taxonId" ), 10 );

            if ( StringUtils.isBlank( likeSymbol ) ) {
                JSONObject json = new JSONObject();
                json.put( "success", false );
                json.put( "message", "Problem with query" );
                jsonText = json.toString();
                log.info( jsonText );
            } else {
                Collection<Researcher> researchers = new ArrayList<>();

                if ( tier == null ) {
                    researchers = researcherService.findByLikeSymbol( taxonId, likeSymbol );
                } else {
                    researchers = researcherService.findByLikeSymbol( taxonId, likeSymbol, tier );
                }
                Set<JSONObject> researchersJson = new HashSet<JSONObject>();
                for ( Researcher r : researchers ) {
                    researchersJson.add( researcherService.toJSON( r ) );
                }
                JSONObject json = new JSONObject();
                json.put( "success", true );
                json.put( "message", "Found " + researchers.size() + " researchers." );
                json.put( "data", (new JSONArray( researchersJson )) );
                jsonText = json.toString();
            }
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
     * AJAX entry point. Find Researchers based on search parameters.
     *
     * @param request
     * @param response
     */
    @RequestMapping("/searchResearchers.html")
    public void searchResearchers( HttpServletRequest request, HttpServletResponse response ) {

        String jsonText = "";
        JSONUtil jsonUtil = new JSONUtil( request, response );

        try {
            String tierSelection = request.getParameter( "tier" );
            String geneIdStr = request.getParameter( "geneId" );
            log.info( tierSelection );
            log.info( geneIdStr );

            TierType tier = StringUtils.isBlank( tierSelection ) ? null : TierType.valueOf( tierSelection );

            Long geneId = Long.parseLong( geneIdStr, 10 );

            Gene gene = geneService.findById( geneId );

            if ( gene == null ) {
                JSONObject json = new JSONObject();
                json.put( "success", false );
                json.put( "message", "Problem with query" );
                jsonText = json.toString();
                log.info( jsonText );
            } else {
                Collection<Researcher> researchers = new ArrayList<>();

                if ( tier == null ) {
                    researchers = researcherService.findByGene( gene );

                } else {
                    researchers = researcherService.findByGene( gene, tier );

                }
                Set<JSONObject> researchersJson = new HashSet<JSONObject>();
                for ( Researcher r : researchers ) {
                    researchersJson.add( researcherService.toJSON( r ) );
                }
                JSONObject json = new JSONObject();
                json.put( "success", true );
                json.put( "message", "Found " + researchers.size() + " researchers." );
                json.put( "data", (new JSONArray( researchersJson )) );
                jsonText = json.toString();
            }
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

}
