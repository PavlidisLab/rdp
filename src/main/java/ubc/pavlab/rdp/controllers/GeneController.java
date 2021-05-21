package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneInfoService;
import ubc.pavlab.rdp.services.TaxonService;

import java.util.List;

import static java.util.function.Function.identity;
import static ubc.pavlab.rdp.util.CollectionUtils.toNullableMap;

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
    @GetMapping(value = "/taxon/{taxonId}/gene/search", params = { "symbols" })
    public Object searchGenesByTaxonAndSymbols( @PathVariable Integer taxonId,
                                                @RequestParam List<String> symbols ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return symbols.stream().collect( toNullableMap( identity(), symbol -> geneService.findBySymbolAndTaxon( symbol, taxon ) ) );
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/gene/search", params = { "query" })
    public Object searchGenesByTaxonAndQuery( @PathVariable Integer taxonId,
                                              @RequestParam String query,
                                              @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return geneService.autocomplete( query, taxon, max );
    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}")
    public Object getGene( @PathVariable Integer geneId ) {
        GeneInfo gene = geneService.load( geneId );
        if ( gene == null ) {
            return ResponseEntity.notFound().build();
        }
        return gene;
    }

    @ResponseBody
    @GetMapping(value = "/gene/{geneId}/term")
    public Object getGeneTerms( @PathVariable Integer geneId ) {
        GeneInfo gene = geneService.load( geneId );
        if ( gene == null ) {
            return ResponseEntity.notFound().build();
        }
        return goService.getTermsForGene( gene, true, true );
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/gene/{symbol}")
    public Object getGeneByTaxonAndSymbol( @PathVariable Integer taxonId, @PathVariable String symbol ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return geneService.findBySymbolAndTaxon( symbol, taxon );
    }
}
