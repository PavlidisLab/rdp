package ubc.pavlab.rdp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CommonsLog
@Secured({ "ROLE_USER", "ROLE_ADMIN" })
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private GOService goService;

    @Data
    static class ProfileWithOrganUberonIds {
        @Valid Profile profile;
        Set<String> organUberonIds;
    }

    @PostMapping(value = "/user/profile")
    public String saveProfile( @RequestBody ProfileWithOrganUberonIds profileWithOrganUberonIds ) {
        log.info(profileWithOrganUberonIds);
        User user = userService.findCurrentUser();
        userService.updateUserProfileAndPublicationsAndOrgans( user, profileWithOrganUberonIds.profile, profileWithOrganUberonIds.profile.getPublications(), Optional.ofNullable( profileWithOrganUberonIds.organUberonIds ) );
        return "Saved.";
    }

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public User getUser() {
        return userService.findCurrentUser();
    }

    @RequestMapping(value = "/user/taxon", method = RequestMethod.GET)
    public Set<Taxon> getTaxons() {
        return userService.findCurrentUser().getUserGenes().values().stream().map( UserGene::getTaxon ).collect( Collectors.toSet() );
    }

    @RequestMapping(value = "/user/gene", method = RequestMethod.GET)
    public Collection<UserGene> getGenes() {
        return userService.findCurrentUser().getUserGenes().values();
    }

    @RequestMapping(value = "/user/term", method = RequestMethod.GET)
    public Collection<UserTerm> getTerms() {
        return userService.findCurrentUser().getUserTerms();
    }

    @Data
    static class Model {

        private Map<Integer, TierType> geneTierMap;
        private Map<Integer, PrivacyLevelType> genePrivacyLevelMap;
        private List<String> goIds;
        private String description;

    }

    @RequestMapping(value = "/user/model/{taxonId}", method = RequestMethod.POST)
    public String saveModelForTaxon( @PathVariable Integer taxonId, @RequestBody Model model ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        user.getTaxonDescriptions().put( taxon, model.getDescription() );

        Map<GeneInfo, TierType> genes = geneService.deserializeGenesTiers( model.getGeneTierMap() );
        Map<GeneInfo, PrivacyLevelType> privacyLevels = geneService.deserializeGenesPrivacyLevels( model.getGenePrivacyLevelMap() );
        Set<GeneOntologyTerm> terms = model.getGoIds().stream()
                .map( s -> goService.getTerm( s ) )
                .collect( Collectors.toSet() );

        userService.updateTermsAndGenesInTaxon( user, taxon, genes, privacyLevels, terms );

        return "Saved.";
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/gene", method = RequestMethod.GET)
    public Set<UserGene> getGenesForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getUserGenes().values().stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) ).collect(Collectors.toSet());
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term", method = RequestMethod.GET)
    public Set<UserTerm> getTermsForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getTermsByTaxon( taxon );
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/search", method = RequestMethod.POST)
    public Map<String, UserTerm> getTermsForTaxon( @PathVariable Integer taxonId, @RequestBody List<String> goIds ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        Set<GeneOntologyTerm> goTerms = goIds.stream().map( goId -> goService.getTerm( goId ) ).collect( Collectors.toSet() );
        return userService.convertTerms( user, taxon, goTerms ).stream().collect( Collectors.toMap( term -> term.getGoId(), term -> term ) );
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/recommend", method = RequestMethod.GET)
    public Object getRecommendedTermsForTaxon( @PathVariable Integer taxonId ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        if ( user == null || taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return userService.recommendTerms( user, taxon );
    }
}
