package ubc.pavlab.rdp.controllers;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
import ubc.pavlab.rdp.util.OntologyMessageSource;
import ubc.pavlab.rdp.util.ParseException;
import ubc.pavlab.rdp.util.TestUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.TreeSet;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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

    @MockBean(name = "reactomeService")
    private ReactomeService reactomeService;

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

    @MockBean
    private OntologyMessageSource ontologyMessageSource;

    @Autowired
    private FormattingConversionService conversionService;

    @Autowired
    private ResourceLoader resourceLoader;

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
        when( ontologyService.resolveOntologyUrl( any( URL.class ) ) ).thenAnswer( a -> resourceLoader.getResource( a.getArgumentAt( 0, URL.class ).toString() ) );
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
    public void getOntology_whenOntologyDoesNotExist_thenReturnNotFound() throws Exception {
        when( ontologyService.findById( 1 ) ).thenReturn( null );
        mvc.perform( get( "/admin/ontologies/{ontologyId}", 1 ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attributeExists( "message" ) );
        verify( ontologyService ).findById( 1 );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology() throws Exception {
        Ontology expectedOntology = Ontology.builder( "mondo" ).id( 1 ).build();
        ArgumentCaptor<Ontology> ontologyArg = ArgumentCaptor.forClass( Ontology.class );
        when( ontologyService.create( ontologyArg.capture() ) ).thenReturn( expectedOntology );
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].termId", "TERM:00001" )
                        .param( "ontologyTerms[0].name", "the best term" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).create( expectedOntology );
        assertThat( ontologyArg.getValue().getTerms() )
                .extracting( "termId" )
                .containsExactly( "TERM:00001" );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenOntologyNameIsEmpty_thenReturnBadRequest() throws Exception {
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasFieldErrorCode( "simpleOntologyForm", "ontologyName", "Size" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenTermsAreJagged_thenReturnBadRequest() throws Exception {
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].termId", "TERM:00001" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasFieldErrorCode( "simpleOntologyForm", "ontologyTerms[0].name", "NotNull" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenTermIdIsMissing_thenUseNextAvailableTermId() throws Exception {
        Ontology expectedOntology = Ontology.builder( "mondo" ).id( 1 ).build();
        ArgumentCaptor<Ontology> ontologyArg = ArgumentCaptor.forClass( Ontology.class );
        when( ontologyService.create( ontologyArg.capture() ) ).thenReturn( expectedOntology );
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].name", "a name" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        assertThat( ontologyArg.getValue().getTerms() ).contains( OntologyTermInfo.builder( expectedOntology, "MONDO:0000001" ).build() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenTermIdIsEmpty_thenUseNextAvailableTermId() throws Exception {
        Ontology expectedOntology = Ontology.builder( "mondo" ).id( 1 ).build();
        ArgumentCaptor<Ontology> ontologyArg = ArgumentCaptor.forClass( Ontology.class );
        when( ontologyService.create( ontologyArg.capture() ) ).thenReturn( expectedOntology );
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].termId", "" )
                        .param( "ontologyTerms[0].name", "a name" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        assertThat( ontologyArg.getValue().getTerms() ).contains( OntologyTermInfo.builder( expectedOntology, "MONDO:0000001" ).build() );
    }


    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenTermIdsAreNonUnique_thenReturnBadRequest() throws Exception {
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].termId", "MONDO:000001" )
                        .param( "ontologyTerms[0].name", "a name" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" )
                        .param( "ontologyTerms[1].termId", "MONDO:000001" )
                        .param( "ontologyTerms[1].name", "a name" )
                        .param( "ontologyTerms[1].grouping", "false" )
                        .param( "ontologyTerms[1].hasIcon", "false" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasFieldErrorCode( "simpleOntologyForm", "ontologyTerms", "AdminController.SimpleOntologyForm.ontologyTerms.nonUniqueTermIds" ) );
    }

    /**
     * FIXME: this occurs when a row is removed and the form is submitted, but we should treat it as an empty row.
     */
    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void createSimpleOntology_whenRowIsMissing_thenTreatItAsAnEmptyRow() throws Exception {
        ArgumentCaptor<Ontology> ontologyArg = ArgumentCaptor.forClass( Ontology.class );
        when( ontologyService.create( ontologyArg.capture() ) ).thenAnswer( a -> {
            Ontology o = a.getArgumentAt( 0, Ontology.class );
            o.setId( 1 );
            return o;
        } );
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .param( "ontologyTerms[0].termId", "MONDO:000001" )
                        .param( "ontologyTerms[0].name", "a name" )
                        .param( "ontologyTerms[0].grouping", "false" )
                        .param( "ontologyTerms[0].hasIcon", "false" )
                        .param( "ontologyTerms[0].active", "false" )
                        .param( "ontologyTerms[2].termId", "MONDO:000002" )
                        .param( "ontologyTerms[2].name", "a name" )
                        .param( "ontologyTerms[2].grouping", "false" )
                        .param( "ontologyTerms[2].hasIcon", "false" )
                        .param( "ontologyTerms[2].active", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).create( any() );
        assertThat( ontologyArg.getValue().getTerms() )
                .extracting( "termId" )
                .containsExactly( "MONDO:000001", "MONDO:000002" );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createSimpleOntology_whenMoreThan20Terms_thenReturnBadRequest() throws Exception {
        MultiValueMap<String, String> termsMap = new LinkedMultiValueMap<>();
        for ( int i = 0; i < 30; i++ ) {
            termsMap.add( "ontologyTerms[" + i + "].termId", "MONDO:000001" );
            termsMap.add( "ontologyTerms[" + i + "].name", "a name" );
            termsMap.add( "ontologyTerms[" + i + "].grouping", "false" );
            termsMap.add( "ontologyTerms[" + i + "].hasIcon", "false" );
            termsMap.add( "ontologyTerms[" + i + "].active", "false" );
        }
        mvc.perform( post( "/admin/ontologies/create-simple-ontology" )
                        .param( "ontologyName", "mondo" )
                        .params( termsMap ) )
                .andExpect( status().isBadRequest() )
                .andExpect( model().attributeHasFieldErrorCode( "simpleOntologyForm", "ontologyTerms", "Size" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateSimpleOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "new name" )
                        .param( "ordering", "2" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).updateNameAndOrderingAndTerms( ontology, "new name", 2, new TreeSet<>() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateSimpleOntology_whenTermIdIsMissingAndExistingTermExists_thenUseNextAvailableTermId() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        ontology.getTerms().add( OntologyTermInfo.builder( ontology, "MONDO:0000057" ).build() );
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "mondo" )
                        .param( "ordering", "2" )
                        .param( "ontologyTerms[0].name", "test" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).updateNameAndOrderingAndTerms( ontology, "mondo", 2, Collections.singleton( OntologyTermInfo.builder( ontology, "MONDO:0000058" ).build() ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateSimpleOntology_whenOrderingIsEmpty_thenUseNull() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "new name" )
                        .param( "ordering", "" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).updateNameAndOrderingAndTerms( ontology, "new name", null, new TreeSet<>() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateSimpleOntology_whenOrderingIsMissing_thenUseNull() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "new name" ) ).andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).updateNameAndOrderingAndTerms( ontology, "new name", null, new TreeSet<>() );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateSimpleOntology_whenMoreThan20Terms_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        MultiValueMap<String, String> termsMap = new LinkedMultiValueMap<>();
        for ( int i = 0; i < 30; i++ ) {
            termsMap.add( "ontologyTerms[" + i + "].termId", "MONDO:000001" );
            termsMap.add( "ontologyTerms[" + i + "].name", "a name" );
            termsMap.add( "ontologyTerms[" + i + "].grouping", "false" );
            termsMap.add( "ontologyTerms[" + i + "].hasIcon", "false" );
            termsMap.add( "ontologyTerms[" + i + "].active", "false" );
        }
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "mondo" )
                        .params( termsMap ) )
                .andExpect( status().isBadRequest() )
                .andExpect( model().attributeHasFieldErrorCode( "simpleOntologyForm", "ontologyTerms", "Size" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateSimpleOntology_whenNewNameIsInvalid_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( ontology.getId() ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-simple-ontology", ontology.getId() )
                        .param( "ontologyName", "" ) )
                .andExpect( status().isBadRequest() );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).countActiveTerms( ontology );
        verify( ontologyService ).countObsoleteTerms( ontology );
        verifyNoMoreInteractions( ontologyService );
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
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update", ontology.getId() ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).countActiveTerms( ontology );
        verify( ontologyService ).countObsoleteTerms( ontology );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void updateOntology_whenResourceCannotBeResolved_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" )
                .id( 1 )
                .ontologyUrl( new URL( "file:/tmp/qei1092u9r4ht" ) )
                .build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update", ontology.getId() ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeExists( "message", "error" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).countActiveTerms( ontology );
        verify( ontologyService ).countObsoleteTerms( ontology );
        verify( ontologyService, VerificationModeFactory.atLeastOnce() ).resolveOntologyUrl( ontology.getOntologyUrl() );
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
    public void importOntology_whenSourceIsFile() throws Exception {
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
    public void importOntology_whenSourceIsEmptyFile_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.createFromObo( any( FileReader.class ) ) ).thenReturn( ontology );
        mvc.perform( fileUpload( "/admin/ontologies/import" )
                        .file( "ontologyFile", new byte[0] ) )
                .andExpect( status().isBadRequest() )
                .andExpect( model().attributeHasErrors( "importOntologyForm" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenSourceIsACompressedFile() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.createFromObo( any( FileReader.class ) ) ).thenReturn( ontology );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( Writer writer = new OutputStreamWriter( new GZIPOutputStream( baos ) ) ) {
            writer.write( "test" );
        }
        mvc.perform( fileUpload( "/admin/ontologies/import" )
                        .file( new MockMultipartFile( "ontologyFile", "mondo.obo.gz", "text/plain", baos.toByteArray() ) ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
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
                .andExpect( model().attributeHasErrors( "importOntologyForm" ) );
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
                .andExpect( model().attributeHasErrors( "importOntologyForm" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenNoSourceAreSupplied_thenReturnBadRequest() throws Exception {
        mvc.perform( post( "/admin/ontologies/import" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasErrors( "importOntologyForm" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenBothOntologyUrlAndFileAreSupplied_thenReturnBadRequest() throws Exception {
        byte[] fileContents = StreamUtils.copyToByteArray( new ClassPathResource( "cache/mondo.obo" ).getInputStream() );
        mvc.perform( fileUpload( "/admin/ontologies/import" )
                        .file( "ontologyFile", fileContents )
                        .param( "ontologyUrl", "file:src/test/resources/cache/mondo.obo" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasErrors( "importOntologyForm" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importOntology_whenFileHasUnsupportedExtension_thenReturnBadRequest() throws Exception {
        mvc.perform( post( "/admin/ontologies/import" )
                        .param( "ontologyUrl", "file:src/test/resources/cache/mondo.owl" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontologies" ) )
                .andExpect( model().attributeHasFieldErrorCode( "importOntologyForm", "ontologyFile", "AdminController.ImportOntologyForm.ontologyFile.unsupportedOntologyFileFormat" ) );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate", ontology.getId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) )
                .andExpect( flash().attribute( "message", "MONDO ontology has been activated." ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).activate( ontology, false );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findTermByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findTermByTermIdAndOntology( "test", ontology );
        verify( ontologyService ).activateTerm( term );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenIncludeSubtree() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findTermByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" )
                        .param( "includeSubtree", "true" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/" + ontology.getId() ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findTermByTermIdAndOntology( "test", ontology );
        verify( ontologyService ).activateTermSubtree( term );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenTermFieldIsEmpty() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        OntologyTermInfo term = OntologyTermInfo.builder( ontology, "test" ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findTermByTermIdAndOntology( "test", ontology ) ).thenReturn( term );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "" )
                        .param( "includeSubtree", "true" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeHasFieldErrors( "activateTermForm", "ontologyTermInfoId" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).countActiveTerms( ontology );
        verify( ontologyService ).countObsoleteTerms( ontology );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void activateOntologyTermInfo_whenTermIsNotInOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( ontologyService.findTermByTermIdAndOntology( "test", ontology ) ).thenReturn( null );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/activate-term", ontology.getId() )
                        .param( "ontologyTermInfoId", "test" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeHasFieldErrorCode( "activateTermForm", "ontologyTermInfoId", "AdminController.ActivateTermForm.unknownTermInOntology" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).findTermByTermIdAndOntology( "test", ontology );
        verify( ontologyService ).countActiveTerms( ontology );
        verify( ontologyService ).countObsoleteTerms( ontology );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void downloadOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        try {
            mvc.perform( get( "/admin/ontologies/{ontologyId}/download", ontology.getId() ) )
                    .andExpect( status().isOk() )
                    .andExpect( content().contentType( MediaType.TEXT_PLAIN ) )
                    .andExpect( header().string( "Content-Disposition", "attachment; filename=mondo.obo" ) );
            verify( ontologyService ).writeObo( eq( ontology ), any( OutputStreamWriter.class ) );
        } catch ( ConcurrentModificationException e ) {
            // FIXME: find the cause if this error, it seems to be a bug in Spring MVC test framework
            Assume.assumeNoException( "This test randomly raises this exception when iterating headers.", e );
        }
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importReactomePathways() throws Exception {
        when( reactomeService.importPathwaysOntology() ).thenReturn( Ontology.builder( "reactome" ).id( 1 ).build() );
        mvc.perform( post( "/admin/ontologies/import-reactome-pathways" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( reactomeService ).findPathwaysOntology();
        verify( reactomeService ).importPathwaysOntology();
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void importReactomePathways_whenReactomeIsAlreadyPresent_thenDoNothing() throws Exception {
        when( reactomeService.findPathwaysOntology() ).thenReturn( Ontology.builder( "reactome" ).id( 1 ).build() );
        mvc.perform( post( "/admin/ontologies/import-reactome-pathways" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) )
                .andExpect( flash().attribute( "message", "The Reactome pathways ontology has already been imported." ) );
        verify( reactomeService ).findPathwaysOntology();
        verifyZeroInteractions( reactomeService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateReactomePathways() throws Exception {
        Ontology reactomeOntology = Ontology.builder( "reactome" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( reactomeOntology );
        when( reactomeService.findPathwaysOntology() ).thenReturn( reactomeOntology );
        when( reactomeService.updatePathwaysOntology() ).thenReturn( reactomeOntology );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-reactome-pathways", 1 ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies/1" ) );
        verify( reactomeService ).findPathwaysOntology();
        verify( reactomeService ).updatePathwaysOntology();
        verifyNoMoreInteractions( reactomeService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateReactomePathways_whenReactomeDoesNotExist_thenReturn404Error() throws Exception {
        when( ontologyService.findById( 1 ) ).thenReturn( Ontology.builder( "reactome" ).id( 1 ).build() );
        when( reactomeService.findPathwaysOntology() ).thenReturn( null );
        mvc.perform( post( "/admin/ontologies/1/update-reactome-pathways" ) )
                .andExpect( status().isNotFound() );
        verify( ontologyService ).findById( 1 );
        verify( reactomeService ).findPathwaysOntology();
        verifyNoMoreInteractions( reactomeService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateReactomePathways_whenSuppliedOntologyIsNotReactome_thenReturn404Error() throws Exception {
        Ontology notReactome = Ontology.builder( "not-reactome" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( notReactome );
        when( reactomeService.findPathwaysOntology() ).thenReturn( Ontology.builder( "reactome" ).id( 2 ).build() );
        mvc.perform( post( "/admin/ontologies/{ontologyId}/update-reactome-pathways", notReactome.getId() ) )
                .andExpect( status().isNotFound() );
        verify( ontologyService ).findById( 1 );
        verify( reactomeService ).findPathwaysOntology();
        verifyNoMoreInteractions( reactomeService );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void updateReactomePathwaySummations() throws Exception {
        Ontology ontology = Ontology.builder( "reactome" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        when( reactomeService.findPathwaysOntology() ).thenReturn( ontology );
        Mockito.doAnswer( ( args ) -> {
            ReactomeService.UpdatePathwaySummationsProgressMonitor pm = args.getArgumentAt( 0, ReactomeService.UpdatePathwaySummationsProgressMonitor.class );
            pm.emit( 1, 4 );
            pm.emit( 2, 4 );
            pm.emit( 3, 4 );
            pm.emit( 4, 4 );
            return null;
        } ).when( reactomeService ).updatePathwaySummations( any() );

        MvcResult mvcResult = mvc.perform( get( "/admin/ontologies/{ontologyId}/update-reactome-pathway-summations", ontology.getId() ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_EVENT_STREAM ) )
                .andExpect( request().asyncStarted() )
                .andReturn();
        // TODO: test the content of the SSE stream

        verify( ontologyService ).findById( 1 );
        verify( reactomeService ).updatePathwaySummations( any() );
    }

    @Test
    public void getOntologyTermPrefix() {
        assertThat( AdminController.getOntologyTermPrefix( Ontology.builder( "uberon" ).build() ) )
                .isEqualTo( "UBERON" );

        assertThat( AdminController.getOntologyTermPrefix( Ontology.builder( "fruits of the garden" ).build() ) )
                .isEqualTo( "FRUITSOFTHEGARDEN" );

        assertThat( AdminController.getOntologyTermPrefix( Ontology.builder( "research-model" ).build() ) )
                .isEqualTo( "RESEARCHMODEL" );

        assertThat( AdminController.getOntologyTermPrefix( Ontology.builder( "Is this really an ontology?" ).build() ) )
                .isEqualTo( "ISTHISREALLYANONTOLOGY" );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteOntology() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( delete( "/admin/ontologies/{ontologyId}", ontology.getId() )
                        .param( "ontologyNameConfirmation", "mondo" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies" ) )
                .andExpect( flash().attributeExists( "message" ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).delete( ontology );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteOntology_whenUserHaveAssociatedTerms_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        doThrow( DataIntegrityViolationException.class ).when( ontologyService ).delete( ontology );
        mvc.perform( delete( "/admin/ontologies/{ontologyId}", ontology.getId() )
                        .param( "ontologyNameConfirmation", "mondo" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( model().attribute( "message", "The MONDO category could not be deleted: null." ) )
                .andExpect( model().attribute( "error", true ) );
        verify( ontologyService ).findById( 1 );
        verify( ontologyService ).delete( ontology );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteOntology_whenNameConfirmationDoesNotMatch_thenReturnBadRequest() throws Exception {
        Ontology ontology = Ontology.builder( "mondo" ).id( 1 ).build();
        when( ontologyService.findById( 1 ) ).thenReturn( ontology );
        mvc.perform( delete( "/admin/ontologies/{ontologyId}", ontology.getId() )
                        .param( "ontologyNameConfirmation", "mond" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/ontology" ) )
                .andExpect( model().attributeHasFieldErrorCode( "deleteOntologyForm", "ontologyNameConfirmation", "AdminController.DeleteOntologyForm.ontologyNameConfirmation.doesNotMatchOntologyName" ) );
        verify( ontologyService ).findById( 1 );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void refreshMessages() throws Exception {
        mvc.perform( post( "/admin/refresh-messages" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/ontologies" ) )
                .andExpect( flash().attribute( "message", "Messages cache have been updated." ) );
    }
}
