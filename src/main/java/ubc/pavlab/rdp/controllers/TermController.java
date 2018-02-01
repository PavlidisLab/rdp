package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserTerm;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;

/**
 * Created by mjacobson on 18/01/18.
 */
@RestController
public class TermController {

    private static Log log = LogFactory.getLog( TermController.class );

    @Autowired
    private GOService goService;

    @Autowired
    private TaxonService taxonService;

    @RequestMapping(value = "/taxon/{taxonId}/term/search/{query}", method = RequestMethod.GET)
    public List<SearchResult<UserTerm>> searchTermsByQueryAndTaxon( @PathVariable Integer taxonId, @PathVariable String query,
                                                                    @RequestParam(value = "max", required = false, defaultValue = "-1") int max) {
        Taxon taxon = taxonService.findById( taxonId );

        return goService.search( query, taxon, max );

    }

    @RequestMapping(value = "/term/{goId}", method = RequestMethod.GET)
    public GeneOntologyTerm getTerm( @PathVariable String goId ) {
        return goService.getTerm( goId );
    }

    @RequestMapping(value = "/taxon/{taxonId}/term/{goId}/gene", method = RequestMethod.GET)
    public Collection<Gene> termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        Taxon taxon = taxonService.findById( taxonId );
        GeneOntologyTerm term = goService.getTerm( goId );

        return goService.getGenes( term, taxon );
    }

}
