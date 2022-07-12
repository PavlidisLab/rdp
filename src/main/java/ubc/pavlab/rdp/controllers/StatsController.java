package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;

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
    public Map<String, Object> getAggregateStats() {
        return new LinkedHashMap<String, Object>() {{
            put( "success", Boolean.TRUE );
            put( "message", "Statistics successfully loaded" );
            put( "researchers_registered", userService.countResearchers() );
            put( "researchers_registered_with_genes", userGeneService.countUsersWithGenes() );
            put( "genes_added", userGeneService.countAssociations() );
            put( "genes_added_unique", userGeneService.countUniqueAssociations() );
            put( "genes_added_unique_alltiers", userGeneService.countUniqueAssociationsAllTiers() );
            put( "human_genes_represented", userGeneService.countUniqueAssociationsToHumanAllTiers() );
            put( "researcher_counts_by_taxon", userGeneService.researcherCountByTaxon() );
        }};
    }


}
