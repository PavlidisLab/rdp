package ubc.pavlab.rdp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by mjacobson on 05/02/18.
 */
@Controller
public class ManagerController {

    private static final String ERR_NO_HOMOLOGUES = "No homologues of %s for specified taxon.";
    private static final String ERR_NO_GENE = "Unknown gene: %s";

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private UserGeneService userGeneService;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = { "nameLike", "iSearch" })
    public ModelAndView searchUsersByName( @RequestParam String nameLike, @RequestParam Boolean iSearch ) {
        User user = userService.findCurrentUser();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "users", userService.findByLikeName( nameLike ) );
        if ( iSearch ) {
            try {
                modelAndView.addObject( "itlUsers", remoteResourceService.findUsersByLikeName( nameLike ) );
            } catch ( RemoteException e ) {
                modelAndView.addObject( "itlErrorMessage", e.getMessage() );
            }
        }
        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = { "nameLike", })
    public ModelAndView searchUsersByNameView( @RequestParam String nameLike ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByLikeName( nameLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view/international", method = RequestMethod.GET, params = { "nameLike" })
    public ModelAndView searchItlUsersByNameView( @RequestParam String nameLike ) {
        if ( !applicationSettings.getIsearch().isEnabled() )
            return null;
        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "users", remoteResourceService.findUsersByLikeName( nameLike ) );
            modelAndView.setViewName( "fragments/user-table :: user-table" );
            modelAndView.addObject( "remote", true );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = { "descriptionLike", "iSearch" })
    public ModelAndView searchUsersByDescription( @RequestParam String descriptionLike,
            @RequestParam Boolean iSearch ) {
        User user = userService.findCurrentUser();
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
        modelAndView.setViewName( "manager/search" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = { "descriptionLike" })
    public ModelAndView searchUsersByDescriptionView( @RequestParam String descriptionLike ) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject( "users", userService.findByDescription( descriptionLike ) );
        modelAndView.setViewName( "fragments/user-table :: user-table" );
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view/international", method = RequestMethod.GET, params = {
            "descriptionLike" })
    public ModelAndView searchItlUsersByDescriptionView( @RequestParam String descriptionLike ) {
        if ( !applicationSettings.getIsearch().isEnabled() )
            return null;
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

    @RequestMapping(value = "/manager/search", method = RequestMethod.GET, params = { "symbol", "taxonId", "tier",
            "iSearch" })
    public ModelAndView searchUsersByGene( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier, @RequestParam Boolean iSearch,
            @RequestParam(name = "homologueTaxonId", required = false) Integer homologueTaxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<Gene> homologues = getHomologuesIfRequested( homologueTaxonId, gene );

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName( "manager/search" );

        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_GENE, symbol ) );
        } else if (
            // Check if there is a homologue request for a different taxon than the original gene
                ( homologueTaxonId != null && !homologueTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some homologue results
                        && ( homologues == null || homologues.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_HOMOLOGUES, symbol ) );
        } else {
            modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier, homologues ) );
            if ( iSearch ) {
                try {
                    modelAndView.addObject( "itlUsergenes",
                            remoteResourceService.findGenesBySymbol( symbol, taxon, tier, homologueTaxonId ) );
                } catch ( RemoteException e ) {
                    modelAndView.addObject( "itlErrorMessage", e.getMessage() );
                }
            }
        }
        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view", method = RequestMethod.GET, params = { "symbol", "taxonId",
            "tier" })
    public ModelAndView searchUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier,
            @RequestParam(name = "homologueTaxonId", required = false) Integer homologueTaxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<Gene> homologues = getHomologuesIfRequested( homologueTaxonId, gene );

        ModelAndView modelAndView = new ModelAndView();
        if ( gene == null ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_GENE, symbol ) );
        } else if (
            // Check if there is a homologue request for a different taxon than the original gene
                ( homologueTaxonId != null && !homologueTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some homologue results
                        && ( homologues == null || homologues.isEmpty() ) ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", String.format( ERR_NO_HOMOLOGUES, symbol ) );
        } else {
            modelAndView.addObject( "usergenes", handleGeneSearch( gene, tier, homologues ) );
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/manager/search/view/international", method = RequestMethod.GET, params = { "symbol",
            "taxonId", "tier" })
    public ModelAndView searchItlUsersByGeneView( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier,
            @RequestParam(name = "homologueTaxonId", required = false) Integer homologueTaxonId ) {
        if ( !applicationSettings.getIsearch().isEnabled() )
            return null;
        Taxon taxon = taxonService.findById( taxonId );

        ModelAndView modelAndView = new ModelAndView();

        try {
            modelAndView.addObject( "usergenes",
                    remoteResourceService.findGenesBySymbol( symbol, taxon, tier, homologueTaxonId ) );
            modelAndView.addObject( "remote", true );
            modelAndView.setViewName( "fragments/user-table :: usergenes-table" );
        } catch ( RemoteException e ) {
            modelAndView.setViewName( "fragments/error :: message" );
            modelAndView.addObject( "errorMessage", e.getMessage() );
        }

        return modelAndView;
    }

    @RequestMapping(value = "/manager/view/{userId}", method = RequestMethod.GET)
    public ModelAndView viewUser( @PathVariable Integer userId ) {
        ModelAndView modelAndView = new ModelAndView();
        User user = userService.findCurrentUser();
        User viewUser = userService.findUserById( userId );

        if ( viewUser == null ) {
            modelAndView.setViewName( "error/404" );
        } else {
            modelAndView.addObject( "user", user );
            modelAndView.addObject( "viewUser", viewUser );
            modelAndView.addObject( "viewOnly", true );
            modelAndView.setViewName( "manager/view" );
        }
        return modelAndView;
    }

    Collection<UserGene> handleGeneSearch( Gene gene, TierType tier, Collection<Gene> homologues ) {
        Collection<UserGene> uGenes = new LinkedList<>();
        if ( homologues != null && !homologues.isEmpty() ) {
            System.out.println("homologues exist: "+homologues.size());
            for ( Gene homologue : homologues ) {
                uGenes.addAll( handleGeneSearch( homologue, tier, null ) );
            }
            return uGenes;
        } else {
            if ( tier.equals( TierType.ANY ) ) {
                return userGeneService.findByGene( gene.getGeneId() );
            } else if ( tier.equals( TierType.MANUAL ) ) {
                return userGeneService.findByGene( gene.getGeneId(), TierType.MANUAL_TIERS );
            } else {
                return userGeneService.findByGene( gene.getGeneId(), tier );
            }
        }
    }

    Collection<Gene> getHomologuesIfRequested( Integer homologueTaxonId, Gene gene ) {
        if ( homologueTaxonId != null ) {
            return geneService.findHomologues( gene, homologueTaxonId );
        }
        //noinspection unchecked
        return Collections.EMPTY_LIST;
    }

}
