package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 22/01/18.
 */
@RestController
public class GeneController {

    private static Log log = LogFactory.getLog( GeneController.class );

    @Autowired
    GeneService geneService;

    @Autowired
    private TaxonService taxonService;

    @RequestMapping(value = "/gene/{taxonId}/search", method = RequestMethod.POST)
    public Collection<Gene> searchGenesByTaxonAndSymbols( @PathVariable Integer taxonId, @RequestBody List<String> symbols ) {
        Taxon taxon = taxonService.findById( taxonId );
        return symbols.stream().map( s -> geneService.findByOfficialSymbolAndTaxon( s, taxon ) ).filter( Objects::nonNull).collect( Collectors.toSet() );
    }

    @RequestMapping(value = "/gene/{taxonId}/search/{query}", method = RequestMethod.GET)
    public Collection<Gene> searchGenesByTaxonAndQuery( @PathVariable Integer taxonId, @PathVariable String query) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.autocomplete( query, taxon );
    }
}
