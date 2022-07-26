package ubc.pavlab.rdp.controllers;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.model.AccessToken;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.ParseException;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.*;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static java.util.function.Function.identity;

@Controller
@CommonsLog
@Secured("ROLE_ADMIN")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private ReactomeService reactomeService;

    @Autowired
    private SiteSettings siteSettings;

    @Autowired
    private SmartValidator smartValidator;

    @Autowired
    private ReloadableResourceBundleMessageSource messageSource;

    /**
     * Task executor used for background tasks.
     * TODO: make this a configurable bean
     */
    private final Executor taskExecutor = Executors.newSingleThreadExecutor();

    /**
     * List all users
     */
    @GetMapping(value = "/admin/users")
    public Object getAllUsers() {
        Collection<User> users = userService.findAll().stream()
                .sorted( Comparator.comparing( u -> u.getProfile().getFullName() ) )
                .collect( Collectors.toList() );
        ModelAndView view = new ModelAndView( "admin/users" );
        view.addObject( "users", users );
        return view;
    }

    @GetMapping(value = "/admin/create-service-account")
    public ModelAndView viewCreateServiceAccount() {
        return new ModelAndView( "admin/create-service-account" )
                .addObject( "user", new User() );
    }

    @PostMapping(value = "/admin/create-service-account")
    public Object createServiceAccount( @Validated(User.ValidationServiceAccount.class) User user, BindingResult bindingResult ) {
        String serviceEmail = user.getEmail() + '@' + siteSettings.getHostUri().getHost();

        if ( userService.findUserByEmailNoAuth( serviceEmail ) != null ) {
            bindingResult.rejectValue( "email", "error.user", "There is already a user registered this email." );
        }

        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/create-service-account", HttpStatus.BAD_REQUEST );
        }

        user.setEmail( serviceEmail );
        user.setEnabled( true );

        Profile profile = user.getProfile();
        profile.setPrivacyLevel( PrivacyLevelType.PRIVATE );
        profile.setShared( false );
        profile.setHideGenelist( false );
        profile.setContactEmailVerified( false );
        user.setProfile( profile );

        user = userService.createServiceAccount( user );

        return "redirect:/admin/users/" + user.getId();
    }

    /**
     * Retrieve a user's details.
     */
    @GetMapping(value = "/admin/users/{user}")
    public Object getUser( @PathVariable User user, @SuppressWarnings("unused") ConfirmEmailForm confirmEmailForm, Locale locale ) {
        if ( user == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.userNotFoundById", null, locale ) );
        }
        return "admin/user";
    }

    @PostMapping(value = "/admin/users/{user}/create-access-token")
    public Object createAccessTokenForUser( @PathVariable User user, RedirectAttributes redirectAttributes, Locale locale ) {
        if ( user == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.userNotFoundById", null, locale ) );
        }
        AccessToken accessToken = userService.createAccessTokenForUser( user );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Successfully created an access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/" + user.getId();
    }

    @PostMapping(value = "/admin/users/{user}/revoke-access-token/{accessToken}")
    public Object revokeAccessTn( @PathVariable User user, @PathVariable AccessToken accessToken, RedirectAttributes redirectAttributes, Locale locale ) {
        if ( user == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.userNotFoundById", null, locale ) );
        }
        if ( accessToken == null || !accessToken.getUser().equals( user ) ) {
            return ResponseEntity.badRequest().build();
        }
        userService.revokeAccessToken( accessToken );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Revoked access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/" + user.getId();
    }

    @Data
    private static class ConfirmEmailForm {
        private String email;
    }

    /**
     * Delete a given user.
     */
    @DeleteMapping(value = "/admin/users/{user}")
    public Object deleteUser( @PathVariable User user,
                              @Valid ConfirmEmailForm confirmEmailForm, BindingResult bindingResult,
                              Locale locale ) {
        if ( user == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.userNotFoundById", null, locale ) );
        }

        if ( !user.getEmail().equals( confirmEmailForm.getEmail() ) ) {
            bindingResult.rejectValue( "email", "error.user.email.doesNotMatchConfirmation", "User email does not match confirmation." );
        }

        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/user", HttpStatus.BAD_REQUEST );
        } else {
            userService.delete( user );
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/admin/ontologies")
    public ModelAndView getOntologies( @SuppressWarnings("unused") ImportOntologyForm importOntologyForm ) {
        return new ModelAndView( "admin/ontologies" )
                .addObject( "simpleOntologyForm", SimpleOntologyForm.withInitialRows( 5 ) );
    }

    @GetMapping("/admin/ontologies/{ontology}")
    public ModelAndView getOntology( @PathVariable Ontology ontology, Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        ModelAndView modelAndView = new ModelAndView( "admin/ontology" )
                .addAllObjects( defaultsForOntologyModelAndView( ontology ) );
        if ( ontologyService.isSimple( ontology ) ) {
            modelAndView.addObject( "simpleOntologyForm", SimpleOntologyForm.fromOntology( ontology ) );
        }
        return modelAndView;
    }

    /**
     * Obtain the defaults for the 'admin/ontology' view to be added via {@link ModelAndView#addAllObjects(Map)}.
     */
    private static Map<String, ?> defaultsForOntologyModelAndView( Ontology ontology ) {
        return new HashMap<String, Object>() {{
            put( "updateOntologyForm", UpdateOntologyForm.fromOntology( ontology ) );
            if ( ontology.getTerms().size() <= OntologyService.SIMPLE_ONTOLOGY_MAX_SIZE ) {
                put( "simpleOntologyForm", SimpleOntologyForm.fromOntology( ontology ) );
            }
            if ( ontology.isActive() ) {
                put( "deactivateOntologyForm", new DeactivateOntologyForm() );
            } else {
                put( "activateOntologyForm", new ActivateOntologyForm() );
            }
            put( "activateTermForm", new ActivateTermForm() );
            put( "deactivateTermForm", new DeactivateTermForm() );
            put( "deleteOntologyForm", new DeleteOntologyForm() );
        }};
    }

    @Data
    private static class UpdateOntologyForm {
        public static UpdateOntologyForm fromOntology( Ontology ontology ) {
            UpdateOntologyForm form = new UpdateOntologyForm();
            form.setName( ontology.getName() );
            form.setOntologyUrl( ontology.getOntologyUrl() );
            form.setAvailableForGeneSearch( ontology.isAvailableForGeneSearch() );
            return form;
        }

        @NotNull
        @NotEmpty
        private String name;

        private URL ontologyUrl;

        private boolean availableForGeneSearch;
    }

    @PostMapping("/admin/ontologies/{ontology}")
    public ModelAndView updateOntology( @PathVariable Ontology ontology,
                                        @Valid UpdateOntologyForm updateOntologyForm, BindingResult bindingResult,
                                        Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }

        ModelAndView modelAndView = new ModelAndView( "admin/ontology" )
                .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                .addObject( "updateOntologyForm", updateOntologyForm );

        if ( ontologyService.isSimple( ontology ) ) {
            modelAndView.addObject( "simpleOntologyForm", SimpleOntologyForm.fromOntology( ontology ) );
        }

        if ( bindingResult.hasErrors() ) {
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            return modelAndView;
        }

        ontology.setAvailableForGeneSearch( updateOntologyForm.isAvailableForGeneSearch() );
        ontologyService.save( ontology );
        modelAndView.addObject( "message", String.format( "Successfully updated %s.", resolveOntologyName( ontology, locale ) ) );

        return modelAndView;
    }

    @DeleteMapping("/admin/ontologies/{ontology}")
    public Object deleteOntology( @PathVariable Ontology ontology,
                                  @Valid DeleteOntologyForm deleteOntologyForm, BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }

        ModelAndView modelAndView = new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                .addObject( "deleteOntologyForm", deleteOntologyForm );

        if ( !deleteOntologyForm.ontologyNameConfirmation.equals( ontology.getName() ) ) {
            bindingResult.rejectValue( "ontologyNameConfirmation", "AdminController.DeleteOntologyForm.ontologyNameConfirmation.doesNotMatchOntologyName" );
        }

        if ( !bindingResult.hasErrors() ) {
            try {
                ontologyService.delete( ontology );
                redirectAttributes.addFlashAttribute( "message", String.format( "The %s category has been deleted.", resolveOntologyName( ontology, locale ) ) );
                return "redirect:/admin/ontologies";
            } catch ( DataIntegrityViolationException e ) {
                log.error( String.format( "An administrator attempted to delete %s, but it failed.", ontology ), e );
                modelAndView.setStatus( HttpStatus.BAD_REQUEST );
                modelAndView.addObject( "error", true );
                modelAndView.addObject( "message", String.format( "The %s category could not be deleted because it is being used. Instead, you should deactivate it.", resolveOntologyName( ontology, locale ) ) );
            }
        }

        return modelAndView;
    }

    @Data
    private static class DeleteOntologyForm {
        @NotNull
        private String ontologyNameConfirmation;
    }

    @PostMapping("/admin/ontologies/create-simple-ontology")
    public Object createSimpleOntology( @SuppressWarnings("unused") ImportOntologyForm importOntologyForm,
                                        @Valid SimpleOntologyForm simpleOntologyForm, BindingResult bindingResult ) {
        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/ontologies", HttpStatus.BAD_REQUEST );
        }
        try {
            Ontology ontology = Ontology.builder( simpleOntologyForm.getOntologyName() )
                    .build();
            ontology.getTerms().addAll( parseTerms( ontology, simpleOntologyForm.getOntologyTerms() ) );
            ontology = ontologyService.create( ontology );
            return "redirect:/admin/ontologies/" + ontology.getId();
        } catch ( OntologyNameAlreadyUsedException e ) {
            bindingResult.rejectValue( "ontologyName", "AdminController.SimpleOntologyForm.ontologyName.alreadyUsed" );
            return new ModelAndView( "admin/ontologies", HttpStatus.BAD_REQUEST );
        }
    }

    @PostMapping("/admin/ontologies/{ontology}/update-simple-ontology")
    public Object updateSimpleOntology( @PathVariable Ontology ontology,
                                        @Valid SimpleOntologyForm simpleOntologyForm, BindingResult bindingResult,
                                        RedirectAttributes redirectAttributes, Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                    .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                    .addObject( "simpleOntologyForm", simpleOntologyForm );
        }
        try {
            ontologyService.updateNameAndTerms( ontology, simpleOntologyForm.ontologyName, parseTerms( ontology, simpleOntologyForm.getOntologyTerms() ) );
            redirectAttributes.addFlashAttribute( "message", String.format( "Ontology %s has been successfully updated.", ontology.getName() ) );
            return "redirect:/admin/ontologies/" + ontology.getId();
        } catch ( OntologyNameAlreadyUsedException e ) {
            bindingResult.rejectValue( "ontologyName", "AdminController.SimpleOntologyForm.ontologyName.alreadyUsed" );
            return new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                    .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                    .addObject( "simpleOntologyForm", simpleOntologyForm );
        }
    }

    @Data
    private static class SimpleOntologyForm {

        public static SimpleOntologyForm fromOntology( Ontology ontology ) {
            if ( ontology.getTerms().size() > OntologyService.SIMPLE_ONTOLOGY_MAX_SIZE ) {
                throw new IllegalArgumentException( "SimpleOntologyForm is not designed to hold ontologies with more than 20 terms!" );
            }
            SimpleOntologyForm updateSimpleForm = new SimpleOntologyForm();
            updateSimpleForm.setOntologyName( ontology.getName() );
            for ( OntologyTermInfo term : ontology.getTerms() ) {
                updateSimpleForm.ontologyTerms.add( new SimpleOntologyTermForm( term.getTermId(), term.getName(), term.isGroup(), term.isHasIcon(), term.isActive() ) );
            }
            return updateSimpleForm;
        }

        public static SimpleOntologyForm withInitialRows( int initialRows ) {
            SimpleOntologyForm updateSimpleForm = new SimpleOntologyForm();
            updateSimpleForm.ontologyName = null;
            for ( int i = 0; i < initialRows; i++ ) {
                updateSimpleForm.ontologyTerms.add( SimpleOntologyTermForm.emptyRow() );
            }
            return updateSimpleForm;
        }

        @NotNull
        @Size(min = 1, max = 255)
        private String ontologyName;

        @Valid
        @Size(max = OntologyService.SIMPLE_ONTOLOGY_MAX_SIZE)
        private List<SimpleOntologyTermForm> ontologyTerms = new ArrayList<>();
    }

    /**
     * Note: unfortunately, this one needs to be public for the purpose of initializing the
     * {@link SimpleOntologyForm#getOntologyTerms()} collection.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimpleOntologyTermForm {

        /**
         * Mark validation rules that apply only when the row is non-empty as per {@link #isEmpty()}.
         */
        public interface RowNotEmpty {

        }

        public static SimpleOntologyTermForm emptyRow() {
            return new SimpleOntologyTermForm( null, "", false, false, false );
        }

        /**
         * Auto-generated if null or empty, which is why we allow zero as a size.
         */
        @Size(max = 255, groups = RowNotEmpty.class)
        private String termId;
        @NotNull(groups = RowNotEmpty.class)
        @Size(min = 1, max = 255, groups = RowNotEmpty.class)
        private String name;
        private boolean grouping;
        private boolean hasIcon;
        private boolean active;

        /**
         * Check if this term is empty and thus should be ignored.
         */
        public boolean isEmpty() {
            return StringUtils.isEmpty( termId ) && StringUtils.isEmpty( name ) && !grouping && !hasIcon && !active;
        }
    }

    private static SortedSet<OntologyTermInfo> parseTerms( Ontology ontology, List<SimpleOntologyTermForm> simpleOntologyTermForms ) {
        OntologyTermInfo lastGroupingTerm = null;
        Map<String, OntologyTermInfo> termById = ontology.getTerms().stream()
                .collect( Collectors.toMap( OntologyTerm::getTermId, identity() ) );
        SortedSet<OntologyTermInfo> terms = new TreeSet<>();
        for ( SimpleOntologyTermForm simpleOntologyTermForm : simpleOntologyTermForms ) {
            if ( simpleOntologyTermForm.isEmpty() ) {
                continue; // empty row, ignore it
            }
            String termId = simpleOntologyTermForm.getTermId();
            OntologyTermInfo term;
            if ( termId == null || termId.isEmpty() ) {
                termId = nextAvailableTermId( ontology, terms );
                term = OntologyTermInfo.builder( ontology, termId ).build();
            } else {
                term = termById.get( termId );
                if ( term == null ) {
                    term = OntologyTermInfo.builder( ontology, termId ).build();
                }
            }
            term.setName( simpleOntologyTermForm.getName() );
            term.setGroup( simpleOntologyTermForm.isGrouping() );
            term.setOrdering( terms.size() + 1 ); // 1-based ordering
            term.setHasIcon( simpleOntologyTermForm.isHasIcon() );
            // terms are always active, unless grouping is set, so they won't appear in autocomplete
            term.setActive( !simpleOntologyTermForm.isGrouping() );
            // this is necessary to proactively evict the ancestors cache
            term.getSuperTerms().clear();
            term.setSubTerms( new TreeSet<>() );
            if ( term.isGroup() ) {
                lastGroupingTerm = term;
            } else if ( lastGroupingTerm != null ) {
                lastGroupingTerm.getSubTerms().add( term );
            }
            terms.add( term );
        }
        return terms;
    }

    /**
     * Infer the ontology term prefix to use for term IDs.
     *
     * @see #nextAvailableTermId(Ontology, Set)
     */
    static String getOntologyTermPrefix( Ontology ontology ) {
        return ontology.getName().replaceAll( "\\W", "" ).toUpperCase();
    }

    /**
     * Generate the next available term ID for a given ontology.
     *
     * @param ontology        ontology from which the term ID is generated
     * @param additionalTerms additional terms, which might not be part of the ontology yet but whose term IDs will be
     *                        used
     * @return a unique, unused term ID
     */
    static String nextAvailableTermId( Ontology ontology, Set<OntologyTermInfo> additionalTerms ) {
        String termPrefix = getOntologyTermPrefix( ontology );
        Pattern termIdPattern = Pattern.compile( "^" + Pattern.quote( termPrefix ) + ":(\\d{7})$" );
        Set<OntologyTermInfo> usedTerms = new HashSet<>();
        usedTerms.addAll( ontology.getTerms() );
        usedTerms.addAll( additionalTerms );
        int maxTermId = usedTerms.stream()
                .map( OntologyTermInfo::getTermId )
                .map( termIdPattern::matcher )
                .filter( Matcher::matches )
                .mapToInt( m -> Integer.parseInt( m.group( 1 ) ) )
                .max().orElse( 0 );
        if ( maxTermId > 9999999 ) {
            // bad news I guess
            throw new IllegalStateException( "The maximum term ID has already been reached." );
        }
        return String.format( "%s:%07d", termPrefix, maxTermId + 1 );
    }

    private class SimpleOntologyFormValidator implements Validator {

        @Override
        public boolean supports( Class<?> clazz ) {
            return SimpleOntologyForm.class.isAssignableFrom( clazz );
        }

        @Override
        public void validate( Object target, Errors errors ) {
            SimpleOntologyForm simpleOntologyForm = (SimpleOntologyForm) target;

            // check if term IDs are unique, ignoring empty rows
            Map<String, Long> countsByTermId = simpleOntologyForm.ontologyTerms.stream()
                    .filter( row -> !row.isEmpty() )
                    .map( SimpleOntologyTermForm::getTermId )
                    .filter( termId -> !StringUtils.isEmpty( termId ) ) // empty or null term IDs are generated
                    .collect( Collectors.groupingBy( t -> t, Collectors.counting() ) );
            if ( countsByTermId.values().stream().anyMatch( x -> x > 1 ) ) {
                errors.rejectValue( "ontologyTerms", "AdminController.SimpleOntologyForm.ontologyTerms.nonUniqueTermIds", "termId must be unique" );
            }

            int i = 0;
            for ( SimpleOntologyTermForm term : simpleOntologyForm.getOntologyTerms() ) {
                // even if converted to false, we are still expecting the non-null error code
                try {
                    errors.pushNestedPath( "ontologyTerms[" + ( i++ ) + "]" );
                    if ( !term.isEmpty() ) {
                        smartValidator.validate( term, errors, SimpleOntologyTermForm.RowNotEmpty.class );
                    }
                } finally {
                    errors.popNestedPath();
                }
            }
        }
    }

    @InitBinder("simpleOntologyForm")
    public void initBinding2( WebDataBinder webDataBinder ) {
        webDataBinder.addValidators( new SimpleOntologyFormValidator() );
    }

    @PostMapping("/admin/ontologies/{ontology}/update")
    public Object updateOntology( @PathVariable Ontology ontology,
                                  RedirectAttributes redirectAttributes,
                                  Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        if ( ontology.getOntologyUrl() == null ) {
            return new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                    .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                    .addObject( "message", "The provided ontology cannot be updated because it lacks an external URL." )
                    .addObject( "error", true );
        } else {
            Resource urlResource = ontologyService.resolveOntologyUrl( ontology.getOntologyUrl() );
            try ( Reader reader = new InputStreamReader( urlResource.getInputStream() ) ) {
                ontologyService.updateFromObo( ontology, reader );
                int numActivated = ontologyService.propagateSubtreeActivation( ontology );
                redirectAttributes.addFlashAttribute( String.format( "Updated %s from %s. %d terms got activated via subtree propagation.", ontology.getName(), ontology.getOntologyUrl(), numActivated ) );
            } catch ( IOException | ParseException e ) {
                log.error( String.format( "Failed to update ontology %s from administrative section.", ontology ), e );
                return new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                        .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                        .addObject( "message", String.format( "Failed to update %s: %s", ontology.getName(), e.getMessage() ) )
                        .addObject( "error", true );
            }
        }
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @PostMapping("/admin/ontologies/import")
    public Object importOntology( @Valid ImportOntologyForm importOntologyForm, BindingResult bindingResult ) {
        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/ontologies", HttpStatus.BAD_REQUEST )
                    .addObject( "simpleOntologyForm", SimpleOntologyForm.withInitialRows( 5 ) );
        }
        try ( Reader reader = getReaderForImportOntologyForm( importOntologyForm ) ) {
            Ontology ontology = ontologyService.createFromObo( reader );
            ontology.setOntologyUrl( importOntologyForm.ontologyUrl );
            ontologyService.save( ontology );
            return "redirect:/admin/ontologies/" + ontology.getId();
        } catch ( OntologyNameAlreadyUsedException e ) {
            bindingResult.reject( "AdminController.ImportOntologyForm.ontologyWithSameNameAlreadyUsed", new String[]{ e.getOntologyName() },
                    String.format( "An ontology with the same name '%s' is already used.", e.getOntologyName() ) );
            return new ModelAndView( "admin/ontologies", HttpStatus.BAD_REQUEST )
                    .addObject( "simpleOntologyForm", SimpleOntologyForm.withInitialRows( 5 ) );
        } catch ( IOException | ParseException e ) {
            log.error( String.format( "Failed to import ontology from submitted form: %s.", importOntologyForm ), e );
            bindingResult.reject( "AdminController.ImportOntologyForm.failedToParseOboFormat", new String[]{ importOntologyForm.getFilename(), e.getMessage() },
                    String.format( "Failed to parse the ontology OBO format from %s: %s", importOntologyForm.getFilename(), e.getMessage() ) );
            return new ModelAndView( "admin/ontologies", HttpStatus.INTERNAL_SERVER_ERROR )
                    .addObject( "simpleOntologyForm", SimpleOntologyForm.withInitialRows( 5 ) );
        }
    }

    /**
     *
     */
    @PostMapping("/admin/ontologies/import-reactome-pathways")
    public Object importReactomePathways( RedirectAttributes redirectAttributes ) {
        Ontology reactomeOntology = reactomeService.findPathwaysOntology();
        if ( reactomeOntology != null ) {
            redirectAttributes.addFlashAttribute( "message", "The Reactome pathways ontology has already been imported." );
            return "redirect:/admin/ontologies/" + reactomeOntology.getId();
        }
        try {
            reactomeOntology = reactomeService.importPathwaysOntology();
            redirectAttributes.addFlashAttribute( "message", "Successfully imported Reactome pathways ontology." );
            return "redirect:/admin/ontologies/" + reactomeOntology.getId();
        } catch ( ReactomeException e ) {
            log.error( "Failed to import Reactome pathways. Could this be an issue with the ontology configuration?", e );
            redirectAttributes.addFlashAttribute( "message", "Failed to import Reactome pathways: " + e.getMessage() + "." );
            redirectAttributes.addFlashAttribute( "error", true );
            return "redirect:/admin/ontologies";
        }
    }

    @PostMapping("/admin/ontologies/{ontology}/update-reactome-pathways")
    public Object updateReactomePathways( @PathVariable Ontology ontology, RedirectAttributes redirectAttributes, Locale locale ) {
        // null-check is not necessary, but can save a database call
        if ( ontology == null || !ontology.equals( reactomeService.findPathwaysOntology() ) ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        try {
            Ontology reactomeOntology = reactomeService.updatePathwaysOntology();
            redirectAttributes.addFlashAttribute( "message", "Successfully updated Reactome pathways ontology." );
            return "redirect:/admin/ontologies/" + reactomeOntology.getId();
        } catch ( ReactomeException e ) {
            log.error( "Failed to update Reactome pathways. Could this be an issue with the ontology configuration?", e );
            redirectAttributes.addFlashAttribute( "message", "Failed to update Reactome pathways: " + e.getMessage() + "." );
            redirectAttributes.addFlashAttribute( "error", true );
            return "redirect:/admin/ontologies";
        }
    }

    /**
     * Update Reactome Pathways summations.
     */
    @PostMapping(value = "/admin/ontologies/{ontology}/update-reactome-pathway-summations")
    public Object updateReactomePathwaySummations( @PathVariable Ontology ontology, Locale locale ) {
        // null-check is not necessary, but can save a database call
        if ( ontology == null || !reactomeService.findPathwaysOntology().equals( ontology ) ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        try {
            reactomeService.updatePathwaySummations( null );
        } catch ( ReactomeException e ) {
            log.error( "Failed to update Reactome pathways summations.", e );
        }
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    /**
     * Unfortunately, this cannot be implemented using a POST method since it's not part of the SSE specification, but
     * we're at least using content negotiation.
     *
     * @see #updateReactomePathwaySummations(Ontology, Locale)
     */
    @GetMapping(value = "/admin/ontologies/{ontology}/update-reactome-pathway-summations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Object updateReactomePathwaySummationsSse( @PathVariable Ontology ontology, Locale locale ) {
        // null-check is not necessary, but can save a database call
        if ( ontology == null || !reactomeService.findPathwaysOntology().equals( ontology ) ) {
            return ResponseEntity.status( HttpStatus.NOT_FOUND )
                    .body( messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        SseEmitter emitter = new SseEmitter( 600 * 1000L );
        emitter.onTimeout( () -> log.warn( "Updating Reactome pathway summations took more time than expected. The SSE stream will be closed, but the update will proceed in the background." ) );
        taskExecutor.execute( new DelegatingSecurityContextRunnable( () -> {
            try {
                reactomeService.updatePathwaySummations( ( progress, maxProgress, elapsedTime ) -> {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                            .data( new ReactomePathwaySummationsUpdateProgress( progress, maxProgress, elapsedTime ), MediaType.APPLICATION_JSON );
                    try {
                        emitter.send( event );
                    } catch ( IOException e ) {
                        log.warn( "Failed to send progress update to client via an SSE stream.", e );
                    }
                } );
                emitter.complete();
            } catch ( ReactomeException e ) {
                log.error( "Failed to update Reactome pathways summations. The progress monitoring stream will be closed.", e );
                emitter.completeWithError( e );
            }
        } ) );
        return emitter;
    }

    @Data
    private static class ReactomePathwaySummationsUpdateProgress {
        private final long processedElements;
        private final long totalElements;
        private final Duration elapsedTime;
    }

    @Data
    private static class ActivateOrDeactivateOntologyForm {
        private boolean includeTerms;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class ActivateOntologyForm extends ActivateOrDeactivateOntologyForm {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class DeactivateOntologyForm extends ActivateOrDeactivateOntologyForm {
    }

    @PostMapping("/admin/ontologies/{ontology}/activate")
    public Object activateOntology( @PathVariable Ontology ontology, ActivateOntologyForm activateOntologyForm, RedirectAttributes redirectAttributes, Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        int activatedTerms = ontologyService.activate( ontology, activateOntologyForm.isIncludeTerms() );
        redirectAttributes.addFlashAttribute( "message", String.format( "%s ontology has been activated.", resolveOntologyName( ontology, locale ) )
                + ( activatedTerms > 0 ? String.format( " In addition, %d terms have been activated.", activatedTerms ) : "" ) );
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @PostMapping("/admin/ontologies/{ontology}/deactivate")
    public Object deactivateOntology( @PathVariable Ontology ontology, DeactivateOntologyForm deactivateOntologyForm, RedirectAttributes redirectAttributes, Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        int numDeactivated = ontologyService.deactivate( ontology, deactivateOntologyForm.isIncludeTerms() );
        redirectAttributes.addFlashAttribute( "message", String.format( "%s ontology has been deactivated.", resolveOntologyName( ontology, locale ) )
                + ( numDeactivated > 0 ? String.format( " In addition, %d terms have been deactivated.", numDeactivated ) : "" ) );
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @Data
    private static class ActivateOrDeactivateTermForm {
        @NotEmpty
        private String ontologyTermInfoId;
        /**
         * Include the subtree spanned by the term.
         */
        private boolean includeSubtree;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class ActivateTermForm extends ActivateOrDeactivateTermForm {
        /**
         * Include the ontology in the activation.
         */
        private boolean includeOntology;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class DeactivateTermForm extends ActivateOrDeactivateTermForm {
    }

    @PostMapping("/admin/ontologies/{ontology}/activate-term")
    public Object activateOntologyTermInfo( @PathVariable Ontology ontology,
                                            @SuppressWarnings("unused") ActivateOntologyForm activateOntologyForm,
                                            @SuppressWarnings("unused") DeleteOntologyForm deleteOntologyForm,
                                            @SuppressWarnings("unused") DeactivateTermForm deactivateTermForm,
                                            @Valid ActivateTermForm activateTermForm, BindingResult bindingResult,
                                            RedirectAttributes redirectAttributes,
                                            Locale locale ) {
        return activateOrDeactivateTerm( ontology, activateTermForm, bindingResult, redirectAttributes, locale );
    }

    @PostMapping("/admin/ontologies/{ontology}/deactivate-term")
    public Object deactivateTerm( @PathVariable Ontology ontology,
                                  @SuppressWarnings("unused") ActivateOntologyForm activateOntologyForm,
                                  @SuppressWarnings("unused") DeleteOntologyForm deleteOntologyForm,
                                  @SuppressWarnings("unused") ActivateTermForm activateTermForm,
                                  @Valid DeactivateTermForm deactivateTermForm, BindingResult bindingResult,
                                  RedirectAttributes redirectAttributes,
                                  Locale locale ) {
        return activateOrDeactivateTerm( ontology, deactivateTermForm, bindingResult, redirectAttributes, locale );
    }

    private Object activateOrDeactivateTerm( Ontology ontology,
                                             ActivateOrDeactivateTermForm activateOrDeactivateTermForm, BindingResult bindingResult,
                                             RedirectAttributes redirectAttributes,
                                             Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        OntologyTermInfo ontologyTermInfo = null;

        if ( !bindingResult.hasFieldErrors( "ontologyTermInfoId" ) ) {
            ontologyTermInfo = ontologyService.findTermByTermIdAndOntology( activateOrDeactivateTermForm.ontologyTermInfoId, ontology );
            if ( ontologyTermInfo == null ) {
                bindingResult.rejectValue( "ontologyTermInfoId", "AdminController.ActivateOrDeactivateTermForm.unknownTermInOntology",
                        new String[]{ activateOrDeactivateTermForm.getOntologyTermInfoId(), ontology.getName() }, null );
            }
        }

        // check if the term belongs to the ontology
        if ( ontologyTermInfo != null && !ontologyTermInfo.getOntology().equals( ontology ) ) {
            bindingResult.rejectValue( "ontologyTermInfoId", "AdminController.ActivateOrDeactivateTermForm.ontologyTermInfoId.termNotPartOfOntology",
                    new String[]{ ontologyTermInfo.getTermId(), ontology.getName() }, null );
        }

        if ( bindingResult.hasErrors() ) {
            return new ModelAndView( "admin/ontology", HttpStatus.BAD_REQUEST )
                    .addAllObjects( defaultsForOntologyModelAndView( ontology ) )
                    .addObject( activateOrDeactivateTermForm instanceof ActivateTermForm ? "activateTermForm" : "deactivateTermForm", activateOrDeactivateTermForm );
        }

        // nullity check is ensured by bindingResult
        assert ontologyTermInfo != null;

        if ( activateOrDeactivateTermForm instanceof ActivateTermForm ) {
            ActivateTermForm activateTermForm = (ActivateTermForm) activateOrDeactivateTermForm;
            if ( activateTermForm.isIncludeSubtree() ) {
                int numActivated = ontologyService.activateTermSubtree( ontologyTermInfo );
                redirectAttributes.addFlashAttribute( "message", String.format( "%d terms under %s subtree in %s has been activated.", numActivated, ontologyTermInfo.getTermId(), ontology.getName() ) );
            } else {
                ontologyService.activateTerm( ontologyTermInfo );
                redirectAttributes.addFlashAttribute( "message", String.format( "%s in %s has been activated.", ontologyTermInfo.getTermId(), ontology.getName() ) );
            }
            // also include the ontology
            if ( activateTermForm.isIncludeOntology() ) {
                ontologyService.activate( ontology, false );
            }
        } else {
            if ( activateOrDeactivateTermForm.isIncludeSubtree() ) {
                int numDeactivated = ontologyService.deactivateTermSubtree( ontologyTermInfo );
                redirectAttributes.addFlashAttribute( "message", String.format( "%d terms under %s subtree in %s has been deactivated.", numDeactivated, ontologyTermInfo.getTermId(), ontology.getName() ) );
            } else {
                ontologyService.deactivateTerm( ontologyTermInfo );
                redirectAttributes.addFlashAttribute( "message", String.format( "%s in %s has been deactivated.", ontologyTermInfo.getTermId(), ontology.getName() ) );
            }
        }

        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    /**
     * Provides the ontology in OBO format.
     */
    @GetMapping(value = "/admin/ontologies/{ontology}/download", produces = "text/plain")
    public ResponseEntity<StreamingResponseBody> downloadOntology( @PathVariable Ontology ontology ) {
        if ( ontology == null ) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType( MediaType.TEXT_PLAIN );
        headers.set( "Content-Disposition", String.format( "attachment; filename=%s.obo", ontology.getName() ) );
        return new ResponseEntity<>( outputStream -> {
            try ( Writer writer = new OutputStreamWriter( outputStream ) ) {
                ontologyService.writeObo( ontology, writer );
            }
        }, headers, HttpStatus.OK );
    }

    @ResponseBody
    @GetMapping("/admin/ontologies/{ontology}/autocomplete-terms")
    public Object autocompleteOntologyTerms( @PathVariable Ontology ontology, @RequestParam String query, Locale locale ) {
        if ( ontology == null ) {
            return ResponseEntity.status( HttpStatus.NOT_FOUND )
                    .body( messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        return ontologyService.autocompleteInactiveTerms( query, ontology, 20, locale );
    }

    @PostMapping("/admin/refresh-messages")
    public String refreshMessages( RedirectAttributes redirectAttributes ) {
        messageSource.clearCacheIncludingAncestors();
        redirectAttributes.addFlashAttribute( "message", "Messages cache have been updated." );
        return "redirect:/admin/ontologies";
    }

    @PostMapping("/admin/ontologies/{ontology}/move")
    public Object move( @PathVariable Ontology ontology, @RequestParam String direction,
                        @SuppressWarnings("unused") ImportOntologyForm importOntologyForm,
                        Locale locale ) {
        if ( ontology == null ) {
            return new ModelAndView( "error/404", HttpStatus.NOT_FOUND )
                    .addObject( "message", messageSource.getMessage( "AdminController.ontologyNotFoundById", null, locale ) );
        }
        OntologyService.Direction d;
        if ( direction.equals( "up" ) ) {
            d = OntologyService.Direction.UP;
        } else if ( direction.equals( "down" ) ) {
            d = OntologyService.Direction.DOWN;
        } else {
            return new ModelAndView( "admin/ontologies", HttpStatus.BAD_REQUEST )
                    .addObject( "error", true )
                    .addObject( "message", String.format( "Invalid direction %s.", direction ) )
                    .addObject( "simpleOntologyForm", SimpleOntologyForm.withInitialRows( 5 ) );
        }
        ontologyService.move( ontology, d );
        return "redirect:/admin/ontologies";
    }

    @Data
    private static class ImportOntologyForm {
        private URL ontologyUrl;
        private MultipartFile ontologyFile;

        public String getFilename() {
            if ( ontologyUrl != null ) {
                // path cannot be null, but it can be empty if missing (i.e. http://github.com)
                return FilenameUtils.getName( ontologyUrl.getPath() );
            } else if ( !isMultipartFileEmpty( ontologyFile ) ) {
                return ontologyFile.getOriginalFilename();
            } else {
                return null;
            }
        }

    }

    /**
     * Obtain a {@link Reader} over the contents of an ontology described by an import form.
     */
    private Reader getReaderForImportOntologyForm( ImportOntologyForm importOntologyForm ) throws IOException {
        InputStream is;
        if ( importOntologyForm.ontologyUrl != null ) {
            is = ontologyService.resolveOntologyUrl( importOntologyForm.ontologyUrl ).getInputStream();
        } else if ( !isMultipartFileEmpty( importOntologyForm.ontologyFile ) ) {
            is = importOntologyForm.ontologyFile.getInputStream();
        } else {
            return null;
        }
        boolean hasGzipExtension = FilenameUtils.isExtension( importOntologyForm.getFilename(), "gz" );
        return new InputStreamReader( hasGzipExtension ? new GZIPInputStream( is ) : is );
    }

    private static boolean isMultipartFileEmpty( MultipartFile mp ) {
        return mp == null || mp.isEmpty();
    }

    private static class ImportOntologyFormValidator implements Validator {

        @Override
        public boolean supports( Class<?> clazz ) {
            return ImportOntologyForm.class.isAssignableFrom( clazz );
        }

        @Override
        public void validate( Object target, Errors errors ) {
            ImportOntologyForm form = (ImportOntologyForm) target;
            if ( form.ontologyUrl == null && isMultipartFileEmpty( form.ontologyFile ) ) {
                errors.reject( "AdminController.ImportOntologyForm.atLeastOnceSourceMustBeProvided" );
            }
            if ( form.ontologyUrl != null && !isMultipartFileEmpty( form.ontologyFile ) ) {
                errors.reject( "AdminController.ImportOntologyForm.urlAndFileCannotCoexist" );
            }
            // check if the filename ends with .obo or .obo.gz
            // the filename can also be empty if there is no original filename attached multipart upload, in which case
            // it's better to just rely on the OBO parser to provide feedback
            // filename can also be null, see MultipartFile.getFilename() specification
            String filename = form.getFilename();
            if ( filename != null && !filename.isEmpty() ) {
                if ( FilenameUtils.isExtension( filename, "gz" ) ) {
                    filename = FilenameUtils.removeExtension( filename );
                }
                if ( !FilenameUtils.isExtension( filename, "obo" ) ) {
                    errors.rejectValue( "ontologyFile", "AdminController.ImportOntologyForm.ontologyFile.unsupportedOntologyFileFormat" );
                }
            }
        }

    }

    @InitBinder("importOntologyForm")
    public void initBinder( WebDataBinder webDataBinder ) {
        webDataBinder.addValidators( new ImportOntologyFormValidator() );
    }

    private String resolveOntologyName( Ontology ontology, Locale locale ) {
        return messageSource.getMessage( "rdp.ontologies." + ontology.getName() + ".title", null, ontology.getName().toUpperCase(), locale );
    }
}
