package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
@CommonsLog
public class SearchController {

    @Autowired
    MessageSource messageSource;

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Autowired
    private PrivacyService privacyService;

    @GetMapping(value = "/search", params = { "nameLike", "iSearch" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam Boolean prefix,
                                           @RequestParam(required = false) Set<String> organUberonIds ) {
        User user = userService.findCurrentUser();
        if(!privacyService.checkUserCanSearch( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix, Optional.ofNullable( organUberonIds ) ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @GetMapping(value = "/search/view", params = { "nameLike", })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike,
                                               @RequestParam Boolean prefix ) {
        if(!privacyService.checkCurrentUserCanSearch( false )){
            return null;
        }
        Collection<User> users = prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike );
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", users );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @GetMapping(value = "/search/view/international", params = {"nameLike"})
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike,
                                                  @RequestParam Boolean prefix,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        if(!privacyService.checkCurrentUserCanSearch(  true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix, Optional.ofNullable( organUberonIds ) ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @GetMapping(value = "/search", params = { "descriptionLike", "iSearch" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
                                                  @RequestParam Boolean iSearch,
                                                  @RequestParam(required = false) Set<String> organUberonIds ) {
        User user = userService.findCurrentUser();
        if(!privacyService.checkUserCanSearch( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        // FIXME: this query should not happen or should be optimized
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike, Optional.ofNullable( organUberonIds ) ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike,
                                                      @RequestParam(required = false) List<String> organUberonIds ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @GetMapping(value = "/search/view/international", params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike,
                                                         @RequestParam(required = false) Set<String> organUberonIds ) {
        if(!privacyService.checkCurrentUserCanSearch( true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike, Optional.ofNullable(organUberonIds) ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @Autowired
    UserOrganService userOrganService;

    private Optional<Collection<UserOrgan>> organsFromUberonIds (Set<String> organUberonIds) {
        Optional<Collection<UserOrgan>> organs = Optional.empty();
        if (organUberonIds != null) {
            organs = Optional.of(userOrganService.findByUberonIdIn( organUberonIds ));
        }
        return organs;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = {"symbol", "taxonId"})
    public ModelAndView searchUsersByGene( @RequestParam String symbol,
                                           @RequestParam Integer taxonId,
                                           @RequestParam Boolean iSearch,
                                           @RequestParam(required = false) Set<TierType> tiers,
                                           @RequestParam(required = false) Integer orthologTaxonId,
                                           @RequestParam(required = false) Set<String> organUberonIds,
                                           Locale locale ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        if (tiers == null) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, taxonService.findById( orthologTaxonId ), organsFromUberonIds( organUberonIds ) );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "search" );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[] {symbol}, locale));
        } else if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[]{ orthologTaxonId.toString() }, locale ) );
        } else {
            modelAndView.addObject( "usergenes", userGeneService.handleGeneSearchWithoutSecurityFilter( gene, tiers, orthologs, organsFromUberonIds( organUberonIds ) ) );
            if ( iSearch ) {
                try {
                    modelAndView.addObject( "itlUsergenes",
                            remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, Optional.ofNullable(organUberonIds) ) );
                } catch ( RemoteException e ) {
                    modelAndView.addObject( "itlErrorMessage", e.getMessage() );
                }
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET)
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol,
                                               @RequestParam Integer taxonId,
                                               @RequestParam(required = false) Set<TierType> tiers,
                                               @RequestParam(required = false) Integer orthologTaxonId,
                                               @RequestParam(required = false) Set<String> organUberonIds,
                                               Locale locale ,
                                               HttpServletRequest req ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }

        if ( tiers == null ) {
            tiers = TierType.ANY;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );

        if (taxon == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );

        if (gene == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[] {symbol}, locale));
            return modelAndView;
        }

        Taxon orthologTaxon = orthologTaxonId == null ? null : taxonService.findById( orthologTaxonId );
        if ( orthologTaxonId != null && orthologTaxon == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologTaxon", new String[] {orthologTaxonId.toString()}, locale));
            return modelAndView;
        }

        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, orthologTaxon, organsFromUberonIds( organUberonIds ) );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[] {symbol}, locale));
            return modelAndView;
        }

        modelAndView.addObject( "usergenes", userGeneService.handleGeneSearch( gene, tiers, orthologs, organsFromUberonIds( organUberonIds ) ) );
        modelAndView.setViewName( "fragments/user-table :: usergenes-table" );

        return modelAndView;
    }

    @Autowired
    GeneInfoService geneInfoService;

    @RequestMapping(value = "/search/view/orthologs", method = RequestMethod.GET)
    public ModelAndView searchOrthologsForGene(@RequestParam String symbol,
                                               @RequestParam Integer taxonId,
                                               @RequestParam(required = false) Set<TierType> tiers,
                                               @RequestParam(required = false) Integer orthologTaxonId,
                                               @RequestParam(required = false) Set<String> organUberonIds,
                                               Locale locale ) {
        ModelAndView modelAndView = new ModelAndView();

        if ( !privacyService.checkCurrentUserCanSearch( false ) ) {
            return null;
        }

        // Only look for orthologs when taxon is human
        if ( taxonId != 9606 ) {
            orthologTaxonId = null;
        }

        if (tiers == null) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        if (taxon == null) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoTaxon", new String[]{ taxonId.toString() }, locale ) );
            return modelAndView;
        }

        GeneInfo gene = geneInfoService.findBySymbolAndTaxon( symbol, taxon );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoGene", new String[]{ symbol }, locale ) );
            return modelAndView;
        }

        Collection<UserGene> orthologs = orthologTaxonId == null ? userGeneService.findOrthologsWithoutSecurityFilter( gene, tiers ) :
                userGeneService.findOrthologsWithTaxonWithoutSecurityFilter( gene, tiers, taxonService.findById( orthologTaxonId ), organsFromUberonIds( organUberonIds ) );

        if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && orthologs.isEmpty() ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage",
                    messageSource.getMessage( "SearchController.errorNoOrthologs", new String[] {symbol}, locale));
            return modelAndView;
        }

        Map<String, List<Gene>> orthologMap = new HashMap<>();
        for (Gene o : orthologs){
            String name = o.getTaxon().getCommonName();
            if (!orthologMap.containsKey(name)) {
                orthologMap.put(name, new ArrayList<Gene>());
            }
            orthologMap.get(name).add(o);
        }

        modelAndView.addObject( "orthologs", orthologMap );
        modelAndView.setViewName( "fragments/ortholog-table :: ortholog-table" );

        return modelAndView;
    }


    @GetMapping(value = "/search/view/international", params = { "symbol", "taxonId", "orthologTaxonId" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol,
                                                  @RequestParam Integer taxonId,
                                                  @RequestParam(required = false) Set<TierType> tiers,
                                                  @RequestParam(required = false) Integer orthologTaxonId,
                                                  @RequestParam(required = false) Set<String> organUberonIds) {
        if(!privacyService.checkCurrentUserCanSearch( true )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            // FIXME: orthologTaxonId = null;
        }

        if (tiers == null) {
            tiers = TierType.ANY;
        }

        Taxon taxon = taxonService.findById( taxonId );
        ModelAndView modelAndView = new ModelAndView();

        try {
            Collection<UserGene> userGenes = remoteResourceService.findGenesBySymbol( symbol, taxon, tiers, orthologTaxonId, Optional.ofNullable( organUberonIds ) );
            modelAndView.addObject( "usergenes", userGenes);
            modelAndView.addObject( "remote", true );
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/userView/{userId}", method = RequestMethod.GET)
    public ModelAndView viewUser( @PathVariable Integer userId,
                                  @RequestParam(required = false) String remoteHost ) {
        if(!privacyService.checkCurrentUserCanSearch(  false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() && privacyService.checkCurrentUserCanSearch( true ) ) {
            try {
                viewUser = remoteResourceService.getRemoteUser( userId, remoteHost );
            } catch ( RemoteException e ) {
                log.error( "Could not fetch the remote user id " + userId + " from " + remoteHost );
                e.printStackTrace();
                return null;
            }
        } else {
            viewUser = userService.findUserById( userId );
        }

        if ( viewUser == null ) {
            modelAndView.setViewName( "error/404" );
        } else {
            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", true );
            modelAndView.setViewName( "userView" );
        }
        return modelAndView;
    }

}
