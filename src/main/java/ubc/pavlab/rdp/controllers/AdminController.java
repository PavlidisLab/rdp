package ubc.pavlab.rdp.controllers;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ubc.pavlab.rdp.model.AccessToken;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyNameAlreadyUsedException;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.services.OntologyStubService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.ParseException;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Controller
@CommonsLog
@Secured("ROLE_ADMIN")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private OntologyStubService ontologyStubService;

    @Autowired
    private SiteSettings siteSettings;

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
    public Object viewCreateServiceAccount( @SuppressWarnings("unused") User user ) {
        return "admin/create-service-account";
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
    public Object getUser( @PathVariable User user, @SuppressWarnings("unused") ConfirmEmailForm confirmEmailForm ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }
        return "admin/user";
    }

    @PostMapping(value = "/admin/users/{user}/create-access-token")
    public Object createAccessTokenForUser( @PathVariable User user, RedirectAttributes redirectAttributes ) {
        AccessToken accessToken = userService.createAccessTokenForUser( user );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Successfully created an access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/{user}";
    }

    @PostMapping(value = "/admin/users/{user}/revoke-access-token/{accessToken}")
    public Object revokeAccessTn( @PathVariable User user, @PathVariable AccessToken accessToken, RedirectAttributes redirectAttributes ) {
        if ( !accessToken.getUser().equals( user ) ) {
            return ResponseEntity.notFound().build();
        }
        userService.revokeAccessToken( accessToken );
        redirectAttributes.addFlashAttribute( "message", MessageFormat.format( "Revoked access token {0}.", accessToken.getToken() ) );
        return "redirect:/admin/users/{user}";
    }

    /**
     * Delete a given user.
     */
    @DeleteMapping(value = "/admin/users/{user}")
    public Object deleteUser( @PathVariable User user,
                              @Validated ConfirmEmailForm confirmEmailForm,
                              BindingResult bindingResult ) {
        if ( user == null ) {
            return ResponseEntity.notFound().build();
        }

        if ( !user.getEmail().equals( confirmEmailForm.getEmail() ) ) {
            bindingResult.rejectValue( "email", "error.user.email.doesNotMatchConfirmation", "User email does not match confirmation." );
        }

        if ( bindingResult.hasErrors() ) {
            ModelAndView view = new ModelAndView( "admin/user" );
            view.setStatus( HttpStatus.BAD_REQUEST );
            return view;
        } else {
            userService.delete( user );
            return "redirect:/admin/users";
        }
    }

    @GetMapping("/admin/ontologies")
    public String getOntologies( @SuppressWarnings("unused") ImportOntologyForm newOntologyForm ) {
        return "admin/ontologies";
    }

    @GetMapping("/admin/ontologies/{ontology}")
    public String getOntology( @SuppressWarnings("unused") Ontology ontology, ActivateTermForm activateTermForm ) {
        return "admin/ontology";
    }

    @PostMapping("/admin/ontologies/{ontology}/update")
    public Object updateOntology( Ontology ontology, ActivateTermForm activateTermForm, RedirectAttributes redirectAttributes ) {
        if ( ontology.getOntologyUrl() == null ) {
            ModelAndView modelAndView = new ModelAndView( "admin/ontology" );
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            return modelAndView;
        } else {
            try ( Reader reader = new InputStreamReader( new UrlResource( ontology.getOntologyUrl() ).getInputStream() ) ) {
                ontologyService.updateFromObo( ontology, reader );
                redirectAttributes.addFlashAttribute( String.format( "Updated %s from %s.", ontology.getName(), ontology.getOntologyUrl() ) );
            } catch ( IOException | ParseException e ) {
                log.error( String.format( "Failed to update ontology %s from administrative section.", ontology ), e );
                redirectAttributes.addFlashAttribute( "message", String.format( "Failed to update %s: %s", ontology.getName(), e.getMessage() ) );
                redirectAttributes.addFlashAttribute( "error", true );
            }
        }
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @PostMapping("/admin/ontologies/create-stubs")
    public Object createStubs( RedirectAttributes redirectAttributes ) {
        ontologyStubService.createStubs();
        redirectAttributes.addFlashAttribute( "message", "Successfully created stubs!" );
        return "redirect:/admin/ontologies";
    }

    @PostMapping("/admin/ontologies/import")
    public Object importOntology( @Valid ImportOntologyForm importOntologyForm, BindingResult bindingResult ) {
        if ( bindingResult.hasErrors() ) {
            return "forward:/admin/ontologies";
        }
        try ( Reader reader = importOntologyForm.getReader() ) {
            Ontology ontology = ontologyService.createFromObo( reader );
            ontology.setOntologyUrl( importOntologyForm.ontologyUrl );
            ontologyService.save( ontology );
            return "redirect:/admin/ontologies/" + ontology.getId();
        } catch ( OntologyNameAlreadyUsedException e ) {
            // happens mainly if the ontology already existed
            // TODO: handle cases where this is not caused by the ontology name unique constraint
            ModelAndView modelAndView = new ModelAndView( "admin/ontologies" );
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            modelAndView.addObject( "message", String.format( "An ontology with the same name '%s' is already used.", e.getOntologyName() ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        } catch ( IOException | ParseException e ) {
            log.error( String.format( "Failed to import ontology from submitted form: %s.", importOntologyForm ), e );
            ModelAndView modelAndView = new ModelAndView( "admin/ontologies" );
            modelAndView.setStatus( HttpStatus.INTERNAL_SERVER_ERROR );
            modelAndView.addObject( "message", String.format( "Failed to parse the ontology OBO format from %s: %s",
                    importOntologyForm.getFilename(), e.getMessage() ) );
            modelAndView.addObject( "error", true );
            return modelAndView;
        }
    }

    @PostMapping("/admin/ontologies/{ontology}/activate")
    public String activateOntology( @PathVariable Ontology ontology, RedirectAttributes redirectAttributes ) {
        ontologyService.activate( ontology );
        redirectAttributes.addFlashAttribute( "message", String.format( "%s ontology has been activated.", ontology.getName() ) );
        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @Data
    private static class ActivateTermForm {
        @NotBlank
        private String ontologyTermInfoId;
        private boolean includeSubtree;
    }

    @PostMapping("/admin/ontologies/{ontology}/activate-term")
    public Object activateOntologyTermInfo( @PathVariable Ontology ontology, @Valid ActivateTermForm activateTermForm, BindingResult bindingResult, RedirectAttributes redirectAttributes ) {
        OntologyTermInfo ontologyTermInfo = null;
        if ( !bindingResult.hasFieldErrors( "ontologyTermInfoId" ) ) {
            ontologyTermInfo = ontologyService.findByTermIdAndOntology( activateTermForm.ontologyTermInfoId, ontology );
            if ( ontologyTermInfo == null ) {
                bindingResult.rejectValue( "ontologyTermInfoId", "", String.format( "Unknown term %s in ontology %s.", activateTermForm.getOntologyTermInfoId(), ontology.getName() ) );
            }
        }

        // check if the term belongs to the ontology
        if ( ontologyTermInfo != null && !ontologyTermInfo.getOntology().equals( ontology ) ) {
            bindingResult.rejectValue( "ontologyTermInfoId", "", String.format( "Term %s is not part of ontology %s.", ontologyTermInfo.getTermId(), ontology.getName() ) );
        }

        if ( bindingResult.hasErrors() ) {
            ModelAndView modelAndView = new ModelAndView( "admin/ontology" );
            modelAndView.setStatus( HttpStatus.BAD_REQUEST );
            return modelAndView;
        }

        // nullity check is ensured by bindingResult
        assert ontologyTermInfo != null;

        if ( activateTermForm.isIncludeSubtree() ) {
            int numActivated = ontologyService.activateTermSubtree( ontologyTermInfo );
            redirectAttributes.addFlashAttribute( "message", String.format( "%d terms under %s subtree in %s has been activated.",
                    numActivated, ontologyTermInfo.getTermId(), ontology.getName() ) );
        } else {
            ontologyService.activateTerm( ontologyTermInfo );
            redirectAttributes.addFlashAttribute( "message", String.format( "%s in %s has been activated.",
                    ontologyTermInfo.getTermId(), ontology.getName() ) );
        }

        return "redirect:/admin/ontologies/" + ontology.getId();
    }

    @Data
    private static class ConfirmEmailForm {
        private String email;
    }

    @Data
    private static class ImportOntologyForm {
        private URL ontologyUrl;
        private MultipartFile ontologyFile;

        public String getFilename() {
            return ontologyUrl != null ? FilenameUtils.getName( ontologyUrl.getPath() ) : ontologyFile.getOriginalFilename();
        }

        public Reader getReader() throws IOException {
            InputStream is;
            if ( ontologyUrl != null ) {
                is = new UrlResource( ontologyUrl ).getInputStream();
            } else if ( ontologyFile != null ) {
                is = ontologyFile.getInputStream();
            } else {
                throw new IllegalStateException( "Either ontologyUrl or ontologyFile must be set." );
            }
            return new InputStreamReader( getFilename().endsWith( ".gz" ) ? new GZIPInputStream( is ) : is );
        }
    }
}
