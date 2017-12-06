package ubc.pavlab.rdp.server.rest;


import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.service.TaxonService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * Created by mjacobson on 04/12/17.
 */
@Controller
public class ResearcherEP {

    private static Log log = LogFactory.getLog( ResearcherEP.class );

    @Autowired
    ResearcherService researcherService;

    @Autowired
    TaxonService taxonService;

    @RequestMapping(value = "/rest/researchers", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> researcherSearch( @RequestParam(value = "name", required = false) final String name,
                                               @RequestParam(value = "taxonId", required = false) final Long taxonId,
                                               @RequestParam(value = "geneSymbol", required = false) final String gene,
                                               @RequestParam(value = "tier", required = false) final String tier ) {
        Collection<Researcher> r;

        if (name != null &&  gene == null && taxonId == null ) {
            r = researcherService.findByLikeName( name );
        } else if ( gene != null && taxonId != null ) {

            Taxon taxon = taxonService.findById( taxonId );
            if ( taxon == null) {
                return new ResponseEntity<>("Unknown taxon.", HttpStatus.BAD_REQUEST);
            }

            if ( StringUtils.isBlank( tier ) ) {
                r = researcherService.findByLikeSymbol( taxonId, gene );
            } else {
                TierType tierType = null;
                try {
                    tierType = TierType.valueOf( tier );
                } catch (IllegalArgumentException e) {
                    return new ResponseEntity<>("Unknown tier.", HttpStatus.BAD_REQUEST);
                }

                r = researcherService.findByLikeSymbol( taxonId, gene, tierType );
            }
            if (name != null) {
                // TODO: Implement a search directly on the database for this type
                Predicate<Researcher> matchesWithRegex = new Predicate<Researcher>() {
                    @Override
                    public boolean apply(Researcher res) {
                        return res.getContact().getFullName().matches("(?i:.*" + name + ".*)");
                    }
                };
                Iterable<Researcher> it = Iterables.filter( r, matchesWithRegex );
                r = Lists.newArrayList( it );
            }
        } else {
            return new ResponseEntity<>( "Invalid parameters.", HttpStatus.BAD_REQUEST );
        }

        return new ResponseEntity<>( r, HttpStatus.OK );
    }

    @RequestMapping(value = "/rest/researchers/{username}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> researcherById( HttpServletRequest request, HttpServletResponse response, @PathVariable String username ) {
        Researcher r = researcherService.findByUserName( username );
        return new ResponseEntity<>( r, HttpStatus.OK );
    }

    @RequestMapping(value = "/rest/my/researcher", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<?> researcherCurrent( HttpServletRequest request, HttpServletResponse response) {
        Researcher r = researcherService.loadCurrentResearcher();
        if (r == null) {
            return new ResponseEntity<>( "Please log in.", HttpStatus.UNAUTHORIZED );
        }
        return new ResponseEntity<>( r, HttpStatus.OK );
    }



}
