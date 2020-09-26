package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;

/**
 * Created by mjacobson on 18/01/18.
 */
@RestController
@CommonsLog
public class TermController {

    private static Role adminRole;

    @Autowired
    private UserService userService;

    @Autowired
    private GOService goService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private RoleRepository roleRepository;

    @PreAuthorize("hasPermission(null, 'search')")
    @RequestMapping(value = "/taxon/{taxonId}/term/search/{query}", method = RequestMethod.GET)
    public List<SearchResult<GeneOntologyTerm>> searchTermsByQueryAndTaxon( @PathVariable Integer taxonId, @PathVariable String query,
                                                                            @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );

        return goService.search( query, taxon, max );

    }

    @PreAuthorize("hasPermission(null, 'search')")
    @RequestMapping(value = "/term/{goId}", method = RequestMethod.GET)
    public GeneOntologyTerm getTerm( @PathVariable String goId ) {
        return goService.getTerm( goId );
    }

    @PreAuthorize("hasPermission(null, 'search')")
    @RequestMapping(value = "/taxon/{taxonId}/term/{goId}/gene", method = RequestMethod.GET)
    public Collection<GeneInfo> termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        Taxon taxon = taxonService.findById( taxonId );
        GeneOntologyTerm term = goService.getTerm( goId );

        return goService.getGenesInTaxon( term, taxon );
    }

}
