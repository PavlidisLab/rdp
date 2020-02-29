package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @RequestMapping(value = "/taxon/{taxonId}/term/search/{query}", method = RequestMethod.GET)
    public List<SearchResult<UserTerm>> searchTermsByQueryAndTaxon( @PathVariable Integer taxonId, @PathVariable String query,
                                                                    @RequestParam(value = "max", required = false, defaultValue = "-1") int max) {
        if( searchNotAuthorized() ){
            return null;
        }
        Taxon taxon = taxonService.findById( taxonId );

        return goService.search( query, taxon, max );

    }

    @RequestMapping(value = "/term/{goId}", method = RequestMethod.GET)
    public GeneOntologyTerm getTerm( @PathVariable String goId ) {
        if( searchNotAuthorized() ){
            return null;
        }
        return goService.getTerm( goId );
    }

    @RequestMapping(value = "/taxon/{taxonId}/term/{goId}/gene", method = RequestMethod.GET)
    public Collection<Gene> termGenes( @PathVariable Integer taxonId, @PathVariable String goId ) {
        if( searchNotAuthorized() ){
            return null;
        }
        Taxon taxon = taxonService.findById( taxonId );
        GeneOntologyTerm term = goService.getTerm( goId );

        return goService.getGenes( term, taxon );
    }

    private boolean searchNotAuthorized() {
        if ( adminRole == null ) {
            adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        }
        User user = userService.findCurrentUser();
        return ( !applicationSettings.getPrivacy().isPublicSearch() // Search is public
                && ( !applicationSettings.getPrivacy().isRegisteredSearch() || user == null )
                // Search is registered and there is user logged
                && ( user == null || adminRole == null || !user.getRoles().contains( adminRole ) ) ); // User is admin
    }

}
