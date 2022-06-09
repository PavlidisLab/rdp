package ubc.pavlab.rdp.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.StreamUtils;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.AccessToken;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.AccessTokenRepository;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.ParseException;
import ubc.pavlab.rdp.util.TestUtils;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createRole;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean(name = "roleRepository")
    private RoleRepository roleRepository;

    @MockBean
    private AccessTokenRepository accessTokenRepository;

    @MockBean(name = "taxonService")
    private TaxonService taxonService;

    @MockBean(name = "userService")
    private UserService userService;

    @MockBean(name = "applicationSettings")
    private ApplicationSettings applicationSettings;

    @MockBean
    ApplicationSettings.PrivacySettings privacySettings;

    @MockBean(name = "siteSettings")
    private SiteSettings siteSettings;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "ontologyService")
    private OntologyService ontologyService;

    @MockBean(name = "ontologyStubService")
    private OntologyStubService ontologyStubService;

    @Autowired
    private FormattingConversionService conversionService;

    private class UserIdToUserConverter implements Converter<String, User> {

        @Override
        public User convert( String s ) {
            return userService.findUserById( Integer.parseInt( s ) );
        }
    }

    private class AccessTokenIdToAccessTokenConverter implements Converter<String, AccessToken> {

        @Override
        public AccessToken convert( String s ) {
            return accessTokenRepository.findOne( Integer.parseInt( s ) );
        }
    }

    private class RoleIdToRoleConverter implements Converter<String, Role> {

        @Override
        public Role convert( String s ) {
            return roleRepository.findOne( Integer.parseInt( s ) );
        }
    }

    private class OntologyIdToOntologyConverter implements Converter<String, Ontology> {
        @Override
        public Ontology convert( String s ) {
            return ontologyService.findById( Integer.parseInt( s ) );
        }
    }

    @Before
    public void setUp() {
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        conversionService.addConverter( new UserIdToUserConverter() );
        conversionService.addConverter( new AccessTokenIdToAccessTokenConverter() );
        conversionService.addConverter( new RoleIdToRoleConverter() );
        conversionService.addConverter( new OntologyIdToOntologyConverter() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedIn_whenCreateServiceAccount_thenRedirect3xx() throws Exception {
        when( siteSettings.getHostUri() ).thenReturn( URI.create( "http://localhost/" ) );
        when( roleRepository.findOne( 1 ) ).thenReturn( createRole( 1, "ROLE_USER" ) );
        when( roleRepository.findOne( 2 ) ).thenReturn( createRole( 2, "ROLE_ADMIN" ) );
        when( userService.createServiceAccount( any() ) ).thenAnswer( answer -> {
            User createdUser = answer.getArgumentAt( 0, User.class );
            createdUser.setId( 1 );
            return createdUser;
        } );
        mvc.perform( post( "/admin/create-service-account" )
                        .param( "profile.name", "Service Account" )
                        .param( "email", "service-account" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/users/1" ) );
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass( User.class );
        verify( userService ).createServiceAccount( captor.capture() );
        assertThat( captor.getValue() )
                .hasFieldOrPropertyWithValue( "profile.name", "Service Account" )
                .hasFieldOrPropertyWithValue( "email", "service-account@localhost" );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedIn_whenCreateAccessToken_thenRedirect3xx() throws Exception {
        User user = createUser( 1 );
        AccessToken accessToken = TestUtils.createAccessToken( 1, user, "1234" );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        when( userService.createAccessTokenForUser( user ) ).thenReturn( accessToken );
        when( roleRepository.findByRole( "ROLE_USER" ) ).thenReturn( createRole( 1, "ROLE_USER" ) );
        mvc.perform( post( "/admin/users/{user}/create-access-token", user.getId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/users/1" ) );
        verify( userService ).createAccessTokenForUser( user );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedIn_whenRevokeAccessToken_thenRedirect3xx() throws Exception {
        User user = createUser( 1 );
        AccessToken accessToken = TestUtils.createAccessToken( 1, user, "1234" );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        when( accessTokenRepository.findOne( 1 ) ).thenReturn( accessToken );
        mvc.perform( post( "/admin/users/{user}/revoke-access-token/{accessToken}", user.getId(), accessToken.getId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/users/1" ) );
        verify( userService ).revokeAccessToken( accessToken );
    }

    @Test
    public void givenNotLoggedIn_whenDeleteUser_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );
        when( userService.findUserById( eq( 1 ) ) ).thenReturn( user );

        mvc.perform( get( "/admin/users/{userId}", user.getId() )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedInAsUser_whenDeleteUser_thenReturn403()
            throws Exception {

        User me = createUser( 1 );

        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( get( "/admin/users/{userId}", me.getId() )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedInAsAdmin_whenDeleteUser_thenSucceed()
            throws Exception {

        User me = createUser( 1 );

        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( delete( "/admin/users/{userId}", me.getId() )
                        .param( "email", me.getEmail() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/users" ) );

        verify( userService ).delete( me );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedInAsAdmin_whenDeleteUserWithWrongConfirmationEmail_thenReturnBadRequest() throws Exception {
        User me = createUser( 1 );

        when( taxonService.findByActiveTrue() ).thenReturn( Collections.emptySet() );
        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( delete( "/admin/users/{userId}", me.getId() )
                        .param( "email", "123@example.com" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/user" ) );

        verify( userService, never() ).delete( me );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void getOntologies() throws Exception {
        mvc.perform( get( "/admin/ontologies" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeExists( "importOntologyForm" ) );
        verify( ontologyService ).findAllOntologiesIncludingInactive();
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void getOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( get( "/admin/ontologies/{ontologyId}", ontology.getId() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "admin/ontology" ) );
        verify( ontologyService ).findById( 1 );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" )
                .id( 1 )
                .ontologyUrl( new URL( "file:src/test/resources/cache/mondo.obo" ) )
                .build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.updateFromObo( any(), any() ) ).thenAnswer( a -> a.getArgumentAt( 0, Ontology.class ) );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update", ontology.getId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).findById( 1 );
        // update is performed from the URL
        verify( ontologyService ).updateFromObo( eq( ontology ), any( InputStreamReader.class ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateOntology_whenOntologyHasNoConfiguredOntologyUrl_thenReturn4xxCode() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update", ontology.getId() ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) );
        verify( ontologyService ).findById( 1 );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenSourceIsUrl() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.createFromObo( any( FileReader.class ) ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/import" )
                        .param( "ontologyUrl", "file:src/test/resources/cache/mondo.obo" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).createFromObo( any( InputStreamReader.class ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_withSourceIsFile() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.createFromObo( any( FileReader.class ) ) ).thenReturn( ontology );
        byte[] fileContents = StreamUtils.copyToByteArray( new ClassPathResource( "cache/mondo.obo" ).getInputStream() );
        mvc.perform( fileUpload( "/admin/ontologies/import" )
                        .file( "ontologyFile", fileContents ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).createFromObo( any( InputStreamReader.class ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenOntologyWithSameNameAlreadyExists_thenReturnBadRequest() throws Exception {
        when( ontologyService.createFromObo( any( Reader.class ) ) ).thenThrow( new OntologyNameAlreadyUsedException( "uberon" ) );
        mvc.perform( post( "/admin/ontologies/import" )
                        .param( "ontologyUrl", "file:src/test/resources/cache/mondo.obo" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attribute( "message", "An ontology with the same name 'uberon' is already used." ) )
                .andExpect( model().attribute( "error", true ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenOntologyIsInvalid_thenReturnBadRequest() throws Exception {
        ParseException parseException = new ParseException( "1234", 0 );
        when( ontologyService.createFromObo( any( Reader.class ) ) ).thenThrow( parseException );
        mvc.perform( post( "/admin/ontologies/import" )
                        .param( "ontologyUrl", "file:src/test/resources/cache/mondo.obo" ) )
                .andExpect( status().isInternalServerError() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attribute( "message", "Failed to parse the ontology OBO format from mondo.obo: " + parseException.getMessage() ) )
                .andExpect( model().attribute( "error", true ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate", ontology.getId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) )
                .andExpect( flash().attribute( "message", "mondo ontology has been activated." ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).activate( ontology );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findByTermIdAndOntology( "test", ontology );
        verify( ontologyService ).activateTerm( term );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenIncludeSubtree() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" )
                        .param( "includeSubtree", "true" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findByTermIdAndOntology( "test", ontology );
        verify( ontologyService ).activateTermSubtree( term );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenTermFieldIsEmpty() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "" )
                        .param( "includeSubtree", "true" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeHasFieldErrors( "activateTermForm", "ontologyTermInfoId" ) );
        verify( ontologyService ).findById( 1 );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenTermIsNotInOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findByTermIdAndOntology( "test", ontology ) ).thenReturn( null );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeHasFieldErrors( "activateTermForm", "ontologyTermInfoId" ) )
                .andExpect( model().attributeHasFieldErrorCode( "activateTermForm", "ontologyTermInfoId", "" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findByTermIdAndOntology( "test", ontology );
        verifyNoMoreInteractions( ontologyService );
    }
}
