package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneInfoService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 22/01/18.
 */
@Controller
@CommonsLog
public class GeneController {

    @Autowired
    GeneInfoService geneService;

    @Autowired
    GOService goService;

    @Autowired
    private TaxonService taxonService;

    @ResponseBody
    @PostMapping(value = "/taxon/{taxonId}/gene/search")
    public Map<String, Gene> searchGenesByTaxonAndSymbols( @PathVariable Integer taxonId, @RequestBody List<String> symbols ) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.findBySymbolInAndTaxon( symbols, taxon )
                .stream()
                .collect( Collectors.toMap( Gene::getSymbol, Function.identity() ) );
        // return symbols.stream().collect( HashMap::new, ( m, s)->m.put(s, geneService.findBySymbolAndTaxon( s, taxon )), HashMap::putAll);
        // return symbols.stream().collect(Collectors.toMap( Function.identity(), s -> geneService.findBySymbolAndTaxon( s, taxon )));
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/gene/search/{query}")
    public Collection<SearchResult<GeneInfo>> searchGenesByTaxonAndQuery( @PathVariable Integer taxonId, @PathVariable String query,
                                                                          @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.autocomplete( query, taxon, max );
    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}")
    public Gene getGene( @PathVariable Integer geneId ) {
        return geneService.load( geneId );
    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}/term")
    public Collection<GeneOntologyTerm> getGeneTerms( @PathVariable Integer geneId ) {
        GeneInfo gene = geneService.load( geneId );
        return goService.getTermsForGene( gene, true, true );
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/gene/{symbol}")
    public Gene getGeneByTaxonAndSymbol( @PathVariable Integer taxonId, @PathVariable String symbol ) {
        Taxon taxon = taxonService.findById( taxonId );
        return geneService.findBySymbolAndTaxon( symbol, taxon );
    }
}
