package ubc.pavlab.rdp.controllers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.UserGene.TierType;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.util.GOTerm;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private static Log log = LogFactory.getLog( UserController.class );

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GOService goService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class PasswordChange {

        @NotEmpty(message = "*Please provide your current password")
        String oldPassword;

        @Length(min = 6, message = "*Your password must have at least 6 characters")
        @NotEmpty(message = "*Please provide a new password")
        String newPassword;

        @Length(min = 6, message = "*Your password must have at least 6 characters")
        @NotEmpty(message = "*Please confirm your password")
        String passwordConfirm;

        @AssertTrue(message = "Passwords should match")
        private boolean isValid() {
            return this.newPassword.equals( this.passwordConfirm );
        }
    }

    @RequestMapping(value = "/user/password", method = RequestMethod.POST)
    public String changePassword( HttpServletResponse response, @RequestBody @Valid PasswordChange passwordChange ) {
        try {
            userService.changePassword( passwordChange.oldPassword, passwordChange.newPassword );
        } catch ( BadCredentialsException e) {
            response.setStatus( HttpServletResponse.SC_UNAUTHORIZED);
            return e.getMessage();
        }
        return "Saved.";
    }

    @RequestMapping(value = "/user/profile", method = RequestMethod.POST)
    public String saveProfile( @RequestBody @Valid Profile profile ) {
        log.info( "/user/profile/save" );
        User user = userService.findCurrentUser();
        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        user.getProfile().setOrganization( profile.getOrganization() );
        user.getProfile().setPhone( profile.getPhone() );

        user.getProfile().getPublications().clear();
        if ( profile.getPublications() != null ) {
            user.getProfile().getPublications().addAll( profile.getPublications() );
        }
        user.getProfile().setWebsite( profile.getWebsite() );

        userService.update( user );
        return "Saved.";
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public User getUser() {
        return userService.findCurrentUser();
    }

    @RequestMapping(value = "/user/taxon", method = RequestMethod.GET)
    public Set<Taxon> getTaxons() {
        return userService.findCurrentUser().getTaxons();
    }

    @RequestMapping(value = "/user/gene", method = RequestMethod.GET)
    public Set<UserGene> getGenes() {
        return userService.findCurrentUser().getGeneAssociations();
    }

    @RequestMapping(value = "/user/term", method = RequestMethod.GET)
    public Set<GeneOntologyTerm> getTerms() {
        return userService.findCurrentUser().getGoTerms();
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/gene", method = RequestMethod.GET)
    public Set<UserGene> getGenesForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getGenesAssociationsByTaxon( taxon );
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/gene", method = RequestMethod.POST)
    public String saveGenesForTaxon( @PathVariable Integer taxonId, @RequestBody Map<Integer, UserGene.TierType> geneTierMap ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        Map<Gene, UserGene.TierType> genes = geneService.deserializeGenes( geneTierMap );

        userService.updateGenesInTaxon( user, taxon, genes );

        return "Done.";
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term", method = RequestMethod.GET)
    public Set<GeneOntologyTerm> getTermsForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getTermsByTaxon( taxon );
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term", method = RequestMethod.POST)
    public String saveTermsForTaxon( @PathVariable Integer taxonId, @RequestBody List<String> goIds ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        Set<GOTerm> terms = goIds.stream().map( s -> goService.getTerm( s ) ).collect( Collectors.toSet() );

        userService.updateGOTermsInTaxon( user, taxon, terms );

        return "Done.";
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/recommend", method = RequestMethod.GET)
    public Collection<GeneOntologyTerm> getRecommendedTermsForTaxon( @PathVariable Integer taxonId ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, new HashSet<>( Arrays.asList( TierType.TIER1, TierType.TIER2 ) ) );
        return goService.convertTermTypes( goService.recommendTerms( genes ), taxon, genes );
    }

}
