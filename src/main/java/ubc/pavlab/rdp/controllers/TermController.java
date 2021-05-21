package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;

/**
 * Created by mjacobson on 18/01/18.
 */
@Controller
@CommonsLog
public class TermController {

    @Autowired
    private GOService goService;

    @Autowired
    private TaxonService taxonService;

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/term/search")
    public Object searchTermsByQueryAndTaxon( @PathVariable Integer taxonId,
                                              @RequestParam String query,
                                              @RequestParam(value = "max", required = false, defaultValue = "-1") int max ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return goService.search( query, taxon, max );

    }

    @ResponseBody
    @GetMapping(value = "/term/{goId}")
    public Object getTerm( @PathVariable String goId ) {
        GeneOntologyTerm term = goService.getTerm( goId );
        if ( term == null ) {
            return ResponseEntity.notFound().build();
        }
        return term;
    }

    @ResponseBody
    @GetMapping(value = "/taxon/{taxonId}/term/{goId}/gene")
    public Object termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        Taxon taxon = taxonService.findById( taxonId );
        if ( taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        GeneOntologyTermInfo term = goService.getTerm( goId );
        if ( term == null ) {
            return ResponseEntity.notFound().build();
        }
        return goService.getGenesInTaxon( term, taxon );
    }

}
