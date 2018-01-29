package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
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
    GOService goService;

    @Autowired
    private TaxonService taxonService;

    @RequestMapping(value = "/taxon/{taxonId}/gene/search", method = RequestMethod.POST)
    public Collection<Gene> searchGenesByTaxonAndSymbols( @PathVariable Integer taxonId, @RequestBody List<String> symbols ) {
        Taxon taxon = taxonService.findById( taxonId );
        return symbols.stream().map( s -> geneService.findBySymbolAndTaxon( s, taxon ) ).filter( Objects::nonNull).collect( Collectors.toSet() );
    }

    @RequestMapping(value = "/taxon/{taxonId}/gene/search/{query}", method = RequestMethod.GET)
    public Collection<Gene> searchGenesByTaxonAndQuery( @PathVariable Integer taxonId, @PathVariable String query) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.autocomplete( query, taxon );
    }

    @RequestMapping(value = "/gene/{geneId}", method = RequestMethod.GET)
    public Gene getGene( @PathVariable Integer geneId) {
        return geneService.load( geneId );
    }

    @RequestMapping(value = "/gene/{geneId}/term", method = RequestMethod.GET)
    public Collection<GeneOntologyTerm> getGeneTerms( @PathVariable Integer geneId) {
        Gene gene=  geneService.load( geneId );
        return goService.getGOTerms( gene );
    }

    @RequestMapping(value = "/taxon/{taxonId}/gene/{symbol}", method = RequestMethod.GET)
    public Gene getGeneByTaxonAndSymbol( @PathVariable Integer taxonId, @PathVariable String symbol) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.findBySymbolAndTaxon( symbol, taxon );
    }
}
