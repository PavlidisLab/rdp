package ubc.pavlab.rdp.controllers;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

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
    private RemoteResourceService remoteResourceService;

    @Autowired
    private RoleRepository roleRepository;

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = { "nameLike", "iSearch" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike, @RequestParam Boolean iSearch, @RequestParam Boolean prefix ) {
        User user = userService.findCurrentUser();
        if(!searchAuthorized( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike, prefix ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET, params = { "nameLike", })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike, @RequestParam Boolean prefix ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", prefix ? userService.findByStartsName( nameLike ) : userService.findByLikeName( nameLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike, @RequestParam Boolean prefix ) {
        if(!searchAuthorized( userService.findCurrentUser(), true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike, prefix ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = { "descriptionLike", "iSearch" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
            @RequestParam Boolean iSearch ) {
        User user = userService.findCurrentUser();
        if(!searchAuthorized( user, false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByDescription( descriptionLike ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "search" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET, params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike ) {
        if(!searchAuthorized( userService.findCurrentUser(), true )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByDescription( descriptionLike ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET, params = { "symbol", "taxonId", "tier", "iSearch" })
    public ModelAndView searchUsersByGene( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier, @RequestParam Boolean iSearch,
            @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<? extends Gene> orthologs = getOrthologsIfRequested( orthologTaxonId, gene );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "search" );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_GENE, symbol ) );
        } else if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_ORTHOLOGS, symbol ) );
        } else {
            modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier, orthologs ) );
            if ( iSearch ) {
                try {
                    modelAndView.addObject( "itlUsergenes",
                            remoteResourceService.findGenesBySymbol( symbol, taxon, tier, orthologTaxonId ) );
                } catch ( RemoteException e ) {
                    modelAndView.addObject( "itlErrorMessage", e.getMessage() );
                }
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "/search/view", method = RequestMethod.GET, params = { "symbol", "taxonId", "tier" })
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier,
            @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<? extends Gene> orthologs = getOrthologsIfRequested( orthologTaxonId, gene );

        ModelAndView modelAndView = new ModelAndView();
        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_GENE, symbol ) );
        } else if (
            // Check if there is a ortholog request for a different taxon than the original gene
                ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some ortholog results
                        && ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_ORTHOLOGS, symbol ) );
        } else {
	    modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier, orthologs ) );
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/search/view/orthologs", method = RequestMethod.GET, params = { "symbol", "taxonId", "tier" })
    public ModelAndView searchOrthologsForGene(@RequestParam String symbol, @RequestParam Integer taxonId,
					       @RequestParam TierType tier,
					       @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<? extends Gene> orthologs = getOrthologsIfRequested( orthologTaxonId, gene );
        Map<String, List<Gene>> orthologMap = null;

        ModelAndView modelAndView = new ModelAndView();

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_GENE, symbol ) );
        } else if (
		   // Check if there is a ortholog request for a different taxon than the original gene
		   ( orthologTaxonId != null && !orthologTaxonId.equals( gene.getTaxon().getId() ) )
		   // Check if we got some ortholog results
		   && ( orthologs == null || orthologs.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_ORTHOLOGS, symbol ) );
        } else {
	    orthologMap = new HashMap<>();
	    for (Gene o : orthologs){
		String name = o.getTaxon().getCommonName();
		if (!orthologMap.containsKey(name)) {
		    orthologMap.put(name, new ArrayList<Gene>());
		} 
		orthologMap.get(name).add(o);		
	    }	    
            modelAndView.addObject( "orthologs", orthologMap );	 
            modelAndView.setViewName( "fragments/ortholog-table :: ortholog-table" );
        }
        return modelAndView;
    }


    
    @RequestMapping(value = "/search/view/international", method = RequestMethod.GET, params = { "symbol", "taxonId", "tier" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier,
            @RequestParam(name = "orthologTaxonId", required = false) Integer orthologTaxonId ) {
        if(!searchAuthorized( userService.findCurrentUser(), true )){
            return null;
        }

        // Only look for orthologs when taxon is human
        if(taxonId != 9606){
            orthologTaxonId = null;
        }

        Taxon taxon = taxonService.findById( taxonId );
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "usergenes",
                    remoteResourceService.findGenesBySymbol( symbol, taxon, tier, orthologTaxonId ) );
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
            @RequestParam(name = "remoteHost", required = false) String remoteHost ) {
        if(!searchAuthorized( userService.findCurrentUser(), false )){
            return null;
        }
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        User viewUser;
        if ( remoteHost != null && !remoteHost.isEmpty() && searchAuthorized( userService.findCurrentUser(), true ) ) {
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

    Collection<UserGene> handleGeneSearch( Gene gene, TierType tier, Collection<Gene> orthologs ) {
        Collection<UserGene> uGenes = new LinkedList<>();
        if ( orthologs != null && !orthologs.isEmpty() ) {
            for ( Gene ortholog : orthologs ) {
                uGenes.addAll( handleGeneSearch( ortholog, tier, null ) );
            }
            return uGenes;
        } else {
            if ( tier.equals( TierType.ANY ) ) {
                return userGeneService.findByGene( gene.getGeneId() );
            } else if ( tier.equals( TierType.TIERS1_2 ) ) {
                return userGeneService.findByGene( gene.getGeneId(), TierType.MANUAL_TIERS );
            } else {
                return userGeneService.findByGene( gene.getGeneId(), tier );
            }
        }
    }

    Collection<Gene> getOrthologsIfRequested( Integer orthologTaxonId, Gene gene ) {
        if ( orthologTaxonId != null ) {
            return userGeneService.findOrthologs( gene, orthologTaxonId );
        }
        //noinspection unchecked
        return Collections.EMPTY_LIST;
    }
    
    private boolean searchAuthorized( User user, boolean international ) {

        if ( adminRole == null ) {
            adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        }

	if ( user == null ){
	    log.info( "User is null in searchAuthorized(); Non-public search will not be authorized." );
	}
	
	
        return ( applicationSettings.getPrivacy().isPublicSearch() // Search is public		 
		 || ( user != null && applicationSettings.getPrivacy().isRegisteredSearch()  ) // Search is registered and there is user logged
		 || ( user != null && adminRole != null && user.getRoles().contains( adminRole ) ) ) // User is admin
                && ( !international || applicationSettings.getIsearch().isEnabled() ); // International search enabled
    }

}
