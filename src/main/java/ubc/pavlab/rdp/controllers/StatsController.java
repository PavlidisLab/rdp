package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mjacobson on 18/01/18.
 */
@Controller
@CommonsLog
public class StatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserGeneService userGeneService;

    /**
     * @deprecated use /api/stats instead
     */
    @Deprecated
    @ResponseBody
    @GetMapping(value = "/stats")
    public Map<String, Object> getAggregateStats( HttpServletResponse response ) {

        Map<String, Object> stats = new LinkedHashMap<>();

        // Backwards compatibility
        response.setHeader( "Access-Control-Allow-Origin", "*" );
        response.setHeader( "Access-Control-Allow-Methods", "GET, POST, DELETE, PUT" );
        response.setHeader( "Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia" );
        stats.put( "success", Boolean.TRUE );
        stats.put( "message", "Statistics successfully loaded" );

        stats.put( "researchers_registered", userService.countResearchers() );
        stats.put( "researchers_registered_with_genes", userGeneService.countUsersWithGenes() );
        stats.put( "genes_added", userGeneService.countAssociations() );
        stats.put( "genes_added_unique", userGeneService.countUniqueAssociations() );
        stats.put( "genes_added_unique_alltiers", userGeneService.countUniqueAssociationsAllTiers() );
        stats.put( "human_genes_represented", userGeneService.countUniqueAssociationsToHumanAllTiers() );
        stats.put( "researcher_counts_by_taxon", userGeneService.researcherCountByTaxon() );

        return stats;
    }


}
