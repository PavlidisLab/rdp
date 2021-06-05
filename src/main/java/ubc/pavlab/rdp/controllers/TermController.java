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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

        Collection<SearchResult<GeneOntologyTermInfo>> foundTerms = goService.search( query, taxon, max );

        // FIXME: this is silly
        for ( SearchResult<GeneOntologyTermInfo> term : foundTerms ) {
            term.getMatch().setSize( goService.getSizeInTaxon( term.getMatch(), taxon ) );
        }

        // sort by size in taxon
        return foundTerms.stream()
                .sorted( Comparator.comparing( result -> result.getMatch().getSize(), Comparator.reverseOrder() ) )
                .collect( Collectors.toList() );
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
