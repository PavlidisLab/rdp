package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserTerm;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;

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
    public Collection<UserTerm> searchTermsByQueryAndTaxon( @PathVariable Integer taxonId, @PathVariable String query ) {
        Taxon taxon = taxonService.findById( taxonId );

        List<GeneOntologyTerm> results = goService.search( query );

        // Only return max 100 hits
        try {
            results = results.subList( 0, 20 );
        } catch (IndexOutOfBoundsException e) {
            // ignore
        }

        // Don't need frequency here
        return goService.convertTermTypes( results, taxon, null );

    }

    @RequestMapping(value = "/taxon/{taxonId}/term/{goId}/gene", method = RequestMethod.GET)
    public Collection<Gene> termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        Taxon taxon = taxonService.findById( taxonId );
        GeneOntologyTerm term = goService.getTerm( goId );

        return goService.getGenes( term, taxon );
    }

}
