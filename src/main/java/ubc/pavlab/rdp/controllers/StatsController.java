package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mjacobson on 18/01/18.
 */
@RestController
@CommonsLog
public class StatsController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserGeneService userGeneService;

    @RequestMapping(value = {"/stats"}, method = RequestMethod.GET)
    public Map<String, Object> getAggregateStats(HttpServletResponse response) {

        Map<String, Object> stats = new LinkedHashMap<>();

        // Backwards compatibility
        response.setHeader( "Access-Control-Allow-Origin", "*" );
        response.setHeader( "Access-Control-Allow-Methods", "GET, POST, DELETE, PUT" );
        response.setHeader( "Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia" );
        stats.put( "success", true );
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
