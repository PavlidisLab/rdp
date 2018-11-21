package ubc.pavlab.rdp.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides API access for remote applications
 */
@Controller
public class ApiController {

    private static final String API_VERSION = "1.0.0";
    private static final Map<String, String> ROOT_DATA;

    static {
        ROOT_DATA = new HashMap<>();
        ROOT_DATA.put( "message", "This is the RDMM REST API. Please see documentation." );
        ROOT_DATA.put( "version", API_VERSION );
    }

    @Autowired
    private UserService userService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private ApplicationSettings applicationSettings;
    @Autowired
    private SiteSettings siteSettings;
    @Autowired
    private ManagerController managerController;

    /**
     * Root endpoint with welcome message and api version.
     *
     * @return 200 and welcome object.
     */
    @RequestMapping(value = "/api", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object apiInfo() {
        return ROOT_DATA;
    }

    /**
     * Fallback for unmapped sub-paths.
     *
     * @return 404.
     */
    @RequestMapping(value = "/api/*", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchApiInfo() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Hides the default users search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @RequestMapping(value = "/api/users/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsers() {
        return ResponseEntity.notFound().build();
    }

    /**
     * Hides the default genes search 400 page when no parameters are provided.
     *
     * @return 404.
     */
    @RequestMapping(value = "/api/genes/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchGenes() {
        return ResponseEntity.notFound().build();
    }

    @RequestMapping(value = "/api/users/search", method = RequestMethod.GET, params = {
            "nameLike" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByName( @RequestParam String nameLike ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        return initUsers( userService.findByLikeName( nameLike ) );
    }

    @RequestMapping(value = "/api/users/search", method = RequestMethod.GET, params = {
            "descriptionLike" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByDescription( @RequestParam String descriptionLike ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        return initUsers( userService.findByDescription( descriptionLike ) );
    }

    @RequestMapping(value = "/api/genes/search", method = RequestMethod.GET, params = { "symbolLike", "taxonId",
            "tier" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByGeneLikeSymbol( @RequestParam String symbolLike, @RequestParam Integer taxonId,
            @RequestParam TierType tier ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        Taxon taxon = taxonService.findById( taxonId );
        return initGeneUsers( managerController.handleGeneSymbolSearch( symbolLike, tier, taxon ) );
    }

    @RequestMapping(value = "/api/genes/search", method = RequestMethod.GET, params = { "symbol", "taxonId",
            "tier" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByGeneSymbol( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        return initGeneUsers( managerController.handleGeneSearch( gene, tier ) );
    }

    private Collection<UserGene> initGeneUsers( Collection<UserGene> genes ) {
        for ( UserGene gene : genes ) {
            //noinspection ResultOfMethodCallIgnored // Initializing for the json serializer.
            gene.getUser().setOrigin( siteSettings.getShortname() );
            gene.getUser().setUserGenes( new HashMap<>() );
            gene.setRemoteUser( gene.getUser() );
        }
        return genes;
    }

    private Collection<User> initUsers( Collection<User> users ) {
        for ( User user : users ) {
            //noinspection ResultOfMethodCallIgnored // Initializing for the json serializer.
            user.setOrigin( siteSettings.getShortname() );
        }
        return users;
    }

}
