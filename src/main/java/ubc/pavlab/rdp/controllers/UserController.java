package ubc.pavlab.rdp.controllers;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
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
@PreAuthorize("isAuthenticated()")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneInfoService geneService;

    @Autowired
    private GOService goService;

    @Autowired
    private OrganInfoService organInfoService;

    @Data
    private static class ProfileWithOrganUberonIds {
        @Valid Profile profile;
        Set<String> organUberonIds;
    }

    @PostMapping(value = "/user/profile")
    public String saveProfile( @RequestBody @Valid ProfileWithOrganUberonIds profileWithOrganUberonIds ) {
        User user = userService.findCurrentUser();
        Profile profile = profileWithOrganUberonIds.profile;
        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        user.getProfile().setOrganization( profile.getOrganization() );
        user.getProfile().setPhone( profile.getPhone() );
        user.getProfile().setWebsite( profile.getWebsite() );
        user.getProfile().setPrivacyLevel( profile.getPrivacyLevel() );
        user.getProfile().setShared( profile.getShared() );
        user.getProfile().setHideGenelist( profile.getHideGenelist() );

        Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn(profileWithOrganUberonIds.organUberonIds).stream()
                .map( organInfo -> user.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo( user, organInfo ) ))
                .collect(Collectors.toMap(ug -> ug.getUberonId(), ug -> ug));
        user.getUserOrgans().clear();
        user.getUserOrgans().putAll( userOrgans );

        userService.updateUserProfileAndPublications( user, profile.getPublications() );

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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
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
        Map<GeneInfo, Optional<PrivacyLevelType>> privacyLevels = geneService.deserializeGenesPrivacyLevels( model.getGenePrivacyLevelMap() );
        Set<GeneOntologyTerm> terms = model.getGoIds().stream().map( s -> goService.getTerm( s ) ).collect( Collectors.toSet() );

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

        return goIds.stream().collect( HashMap::new, ( m, s)->m.put(s, userService.convertTerms( user, taxon, goService.getTerm( s ) )), HashMap::putAll);
    }

    @RequestMapping(value = "/user/taxon/{taxonId}/term/recommend", method = RequestMethod.GET)
    public Collection<UserTerm> getRecommendedTermsForTaxon( @PathVariable Integer taxonId ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        Collection<UserTerm> terms = userService.recommendTerms( user, taxon );

        return terms;
    }
}
