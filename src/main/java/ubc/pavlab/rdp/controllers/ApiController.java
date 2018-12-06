package ubc.pavlab.rdp.controllers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.exception.TierException;
import ubc.pavlab.rdp.model.*;
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

    private static final Log log = LogFactory.getLog( ApiController.class );
    private static final String API_VERSION = "1.0.0";
    private static final String MISCONF_REMOTE_ADMIN = "The remote admin account is misconfigured! Remote searches won't be able to authenticate even with valid security tokens!";
    private static final Map<String, String> ROOT_DATA;
    private static final ResponseEntity<String> TIER3_RESPONSE = new ResponseEntity<>(
            "Tier3 genes not published internationally.", null, HttpStatus.NOT_FOUND );
    private static final ResponseEntity<String> GENE_NULL_RESPONSE = new ResponseEntity<>( "Unknown gene.", null,
            HttpStatus.NOT_FOUND );
    private static final ResponseEntity<String> HOMOLOGUE_NULL_RESPONSE = new ResponseEntity<>(
            "Could not find any homologues with given parameters.", null, HttpStatus.NOT_FOUND );

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
    public Object searchUsersByName( @RequestParam String nameLike,
            @RequestParam(name = "auth", required = false) String auth ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        return initUsers( userService.findByLikeName( nameLike ) );
    }

    @RequestMapping(value = "/api/users/search", method = RequestMethod.GET, params = {
            "descriptionLike" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByDescription( @RequestParam String descriptionLike,
            @RequestParam(name = "auth", required = false) String auth ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        return initUsers( userService.findByDescription( descriptionLike ) );
    }

    @RequestMapping(value = "/api/genes/search", method = RequestMethod.GET, params = { "symbol", "taxonId",
            "tier" }, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object searchUsersByGeneSymbol( @RequestParam String symbol, @RequestParam Integer taxonId,
            @RequestParam TierType tier, @RequestParam(name = "auth", required = false) String auth,
            @RequestParam(name = "homologueTaxonId", required = false) Integer homologueTaxonId ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        Taxon taxon = taxonService.findById( taxonId );
        Gene gene = geneService.findBySymbolAndTaxon( symbol, taxon );
        Collection<Gene> homologues = managerController.getHomologuesIfRequested( homologueTaxonId, gene );

        if ( gene == null ) {
            return GENE_NULL_RESPONSE;
        } else if (
            // Check if there is a homologue request for a different taxon than the original gene
                ( homologueTaxonId != null && !homologueTaxonId.equals( gene.getTaxon().getId() ) )
                        // Check if we got some homologue results
                        && ( homologues == null || homologues.isEmpty() ) ) {
            return HOMOLOGUE_NULL_RESPONSE;
        }

        try {
            return initGeneUsers( managerController.handleGeneSearch( gene, restrictTiers( tier ), homologues ) );
        } catch ( TierException e ) {
            return TIER3_RESPONSE;
        }
    }

    @RequestMapping(value = "/api/users/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON)
    @ResponseBody
    public Object getUserById( @PathVariable Integer userId,
            @RequestParam(name = "auth", required = false) String auth ) {
        if ( !applicationSettings.getIsearch().isEnabled() ) {
            return ResponseEntity.notFound().build();
        }
        checkAuth( auth );
        User user = userService.findUserById( userId );
        if(user == null){
            return ResponseEntity.notFound().build();
        }
        return initUser( user );
    }

    private void checkAuth( String auth ) {
        if ( auth == null || auth.length() < 1 || applicationSettings.getIsearch().getAuthTokens() == null
                || !applicationSettings.getIsearch().getAuthTokens().contains( auth ) ) {
            SecurityContextHolder.getContext().setAuthentication( null );
        } else if ( applicationSettings.getIsearch().getAuthTokens().contains( auth ) ) {
            User u = userService.getRemoteAdmin();
            if ( u == null ) {
                log.error( MISCONF_REMOTE_ADMIN );
                return;
            }
            UserPrinciple principle = new UserPrinciple( u );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken( principle, null, principle.getAuthorities() ) );
        }
    }

    private Collection<UserGene> initGeneUsers( Collection<UserGene> genes ) {
        for ( UserGene gene : genes ) {
            //noinspection ResultOfMethodCallIgnored // Initializing for the json serializer.
            initUser( gene.getUser() );
            gene.getUser().setUserGenes( new HashMap<>() );
            gene.setRemoteUser( gene.getUser() );
        }
        return genes;
    }

    private Collection<User> initUsers( Collection<User> users ) {
        for ( User user : users ) {
            this.initUser( user );
        }
        return users;
    }

    private User initUser(User user){
        user.setOrigin( siteSettings.getShortname() );
        user.setOriginUrl( siteSettings.getFullUrl() );
        return user;
    }

    /**
     * We do not want to query TIER3 genes internationally, so if such request arrives, we have to either
     * try to transform it to only include TIER1&2, or prevent the search.
     *
     * @param tier the tier type to be restricted to not include tier 3.
     * @return manual (tier1&2) for tier type ANY, or throws an exception if tier type was specifically 3.
     */
    private TierType restrictTiers( TierType tier ) throws TierException {
        switch ( tier ) {
            case ANY:
                return TierType.MANUAL;
            case TIER3:
                throw new TierException( "TIER3 not allowed for international search" );
            default:
                return tier;
        }
    }

}
