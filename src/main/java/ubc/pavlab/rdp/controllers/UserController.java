package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Controller
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

    @Autowired
    private EmailService emailService;

    @GetMapping(value = { "/user/home" })
    public ModelAndView userHome() {
        ModelAndView modelAndView = new ModelAndView( "user/home" );
        modelAndView.addObject( "user", userService.findCurrentUser() );
        return modelAndView;
    }

    @GetMapping(value = { "/user/model/{taxonId}" })
    public ModelAndView model( @PathVariable Integer taxonId ) {
        ModelAndView modelAndView = new ModelAndView( "user/model" );
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        modelAndView.addObject( "viewOnly", null );
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "taxon", taxon );
        return modelAndView;
    }

    @GetMapping(value = "/user/taxon/{taxonId}/term/{goId}/gene/view")
    public ModelAndView getTermsGenesForTaxon( @PathVariable Integer taxonId, @PathVariable String goId ) {
        ModelAndView modelAndView = new ModelAndView( "fragments/gene-table::gene-table" );
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        GeneOntologyTerm term = goService.getTerm( goId );

        if ( term != null ) {
            Set<Integer> geneIds = goService.getGenes( term ).stream().map( GeneInfo::getGeneId ).collect( Collectors.toSet() );
            modelAndView.addObject( "genes",
                    user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ).stream()
                            .filter( ug -> geneIds.contains( ug.getGeneId() ) )
                            .collect( Collectors.toSet() ) );
        } else {
            modelAndView.addObject( "genes", Collections.EMPTY_SET );
        }
        modelAndView.addObject( "viewOnly", true );
        return modelAndView;
    }

    @GetMapping(value = { "/user/profile" })
    public ModelAndView profile() {
        ModelAndView modelAndView = new ModelAndView( "user/profile" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        modelAndView.addObject( "viewOnly", null );
        return modelAndView;
    }

    @GetMapping(value = { "/user/documentation" })
    public ModelAndView documentation() {
        ModelAndView modelAndView = new ModelAndView( "user/documentation" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        return modelAndView;
    }

    @GetMapping(value = { "/user/faq" })
    public ModelAndView faq() {
        ModelAndView modelAndView = new ModelAndView( "user/faq" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        return modelAndView;
    }

    @GetMapping(value = { "/user/support" })
    public ModelAndView support() {
        ModelAndView modelAndView = new ModelAndView( "user/support" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );
        return modelAndView;
    }

    @PostMapping(value = { "/user/support" })
    public ModelAndView supportPost( HttpServletRequest request,
                                     @RequestParam String name,
                                     @RequestParam String message,
                                     @RequestParam(required = false) MultipartFile attachment ) {
        ModelAndView modelAndView = new ModelAndView( "user/support" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        log.info( MessageFormat.format( "{0} is attempting to contact support.", user ) );

        try {
            emailService.sendSupportMessage( message, name, user, request, attachment );
            modelAndView.addObject( "message", "Sent. We will get back to you shortly." );
            modelAndView.addObject( "success", true );
        } catch ( MessagingException e ) {
            log.error( e );
            modelAndView
                    .addObject( "message", "There was a problem sending the support request. Please try again later." );
            modelAndView.addObject( "success", false );
        }

        return modelAndView;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PasswordChange extends PasswordReset {

        @NotEmpty(message = "Current password cannot be empty.")
        private String oldPassword;
    }

    @GetMapping(value = "/user/password")
    public ModelAndView changePassword() {
        ModelAndView modelAndView = new ModelAndView( "user/password" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        modelAndView.addObject( "passwordChange", new PasswordChange() );
        return modelAndView;
    }

    @PostMapping(value = "/user/password")
    public ModelAndView changePassword( @Valid PasswordChange passwordChange, BindingResult bindingResult ) {
        ModelAndView modelAndView = new ModelAndView( "user/password" );
        User user = userService.findCurrentUser();
        modelAndView.addObject( "user", user );

        if ( !passwordChange.isValid() ) {
            bindingResult.rejectValue( "passwordConfirm", "error.passwordChange", "Password conformation does not match new password." );
        }

        if ( bindingResult.hasErrors() ) {
            // Short circuit before testing password.
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            return modelAndView;
        }

        try {
            userService.changePassword( passwordChange.getOldPassword(), passwordChange.getNewPassword() );
        } catch ( BadCredentialsException e ) {
            bindingResult.rejectValue( "oldPassword", "error.passwordChange", "Current password does not match." );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
        } else {
            modelAndView.addObject( "passwordChange", new PasswordChange() );
            modelAndView.addObject( "message", "Password Updated" );
        }

        return modelAndView;
    }

    @GetMapping("/user/verify-contact-email")
    public ModelAndView verifyContactEmail( @RequestParam String token ) {
        ModelAndView modelAndView = new ModelAndView();
        try {
            userService.confirmVerificationToken( token );
            modelAndView.setViewName( "redirect:/user/home" );
        } catch ( TokenException e ) {
            modelAndView.setStatus( HttpStatus.NOT_FOUND );
            modelAndView.setViewName( "error/404" );
            log.error( e );
        }
        return modelAndView;
    }

    @Data
    static class ProfileWithOrganUberonIds {
        @Valid
        Profile profile;
        Set<String> organUberonIds;
    }

    @ResponseBody
    @PostMapping(value = "/user/profile", produces = MediaType.TEXT_PLAIN_VALUE)
    public String saveProfile( @RequestBody ProfileWithOrganUberonIds profileWithOrganUberonIds ) {
        User user = userService.findCurrentUser();
        userService.updateUserProfileAndPublicationsAndOrgans( user, profileWithOrganUberonIds.profile, profileWithOrganUberonIds.profile.getPublications(), profileWithOrganUberonIds.organUberonIds );
        return "Saved.";
    }

    @ResponseBody
    @GetMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
    public User getUser() {
        return userService.findCurrentUser();
    }

    @ResponseBody
    @GetMapping(value = "/user/taxon", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<Taxon> getTaxons() {
        return userService.findCurrentUser().getUserGenes().values().stream().map( UserGene::getTaxon ).collect( Collectors.toSet() );
    }

    @ResponseBody
    @GetMapping(value = "/user/gene", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<UserGene> getGenes() {
        return userService.findCurrentUser().getUserGenes().values();
    }

    @ResponseBody
    @GetMapping(value = "/user/term", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @ResponseBody
    @PostMapping(value = "/user/model/{taxonId}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String saveModelForTaxon( @PathVariable Integer taxonId, @RequestBody Model model ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );

        user.getTaxonDescriptions().put( taxon, model.getDescription() );

        Map<GeneInfo, TierType> genes = model.getGeneTierMap().keySet().stream()
                .filter( geneId -> model.getGeneTierMap().get( geneId ) != null ) // strip null values
                .map( geneId -> geneService.load( geneId ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toMap( identity(), g -> model.getGeneTierMap().get( g.getGeneId() ) ) );

        Map<GeneInfo, PrivacyLevelType> privacyLevels = model.getGenePrivacyLevelMap().keySet().stream()
                .filter( geneId -> model.getGenePrivacyLevelMap().get( geneId ) != null ) // strip null values
                .map( geneId -> geneService.load( geneId ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toMap( identity(), g -> model.getGenePrivacyLevelMap().get( g.getGeneId() ) ) );

        Set<GeneOntologyTerm> terms = model.getGoIds().stream()
                .map( s -> goService.getTerm( s ) )
                .collect( Collectors.toSet() );

        userService.updateTermsAndGenesInTaxon( user, taxon, genes, privacyLevels, terms );

        return "Saved.";
    }

    @ResponseBody
    @GetMapping(value = "/user/taxon/{taxonId}/gene", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<UserGene> getGenesForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getUserGenes().values().stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @ResponseBody
    @GetMapping(value = "/user/taxon/{taxonId}/term", produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<UserTerm> getTermsForTaxon( @PathVariable Integer taxonId ) {
        Taxon taxon = taxonService.findById( taxonId );
        return userService.findCurrentUser().getTermsByTaxon( taxon );
    }

    @ResponseBody
    @PostMapping(value = "/user/taxon/{taxonId}/term/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, UserTerm> getTermsForTaxon( @PathVariable Integer taxonId, @RequestBody List<String> goIds ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        Set<GeneOntologyTerm> goTerms = goIds.stream().map( goId -> goService.getTerm( goId ) ).collect( Collectors.toSet() );
        return userService.convertTerms( user, taxon, goTerms ).stream().collect( Collectors.toMap( GeneOntologyTerm::getGoId, term -> term ) );
    }

    @ResponseBody
    @GetMapping(value = "/user/taxon/{taxonId}/term/recommend", produces = MediaType.APPLICATION_JSON_VALUE)
    public Object getRecommendedTermsForTaxon( @PathVariable Integer taxonId ) {
        User user = userService.findCurrentUser();
        Taxon taxon = taxonService.findById( taxonId );
        if ( user == null || taxon == null ) {
            return ResponseEntity.notFound().build();
        }
        return userService.recommendTerms( user, taxon );
    }
}
