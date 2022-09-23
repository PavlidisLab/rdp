package ubc.pavlab.rdp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.exception.UnknownRemoteApiException;
import ubc.pavlab.rdp.listeners.UserListener;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.RemoteOntology;
import ubc.pavlab.rdp.security.Permissions;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
@WebMvcTest({ SearchController.class, SearchViewController.class })
public class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "applicationSettings")
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.ProfileSettings profileSettings;

    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;

    @MockBean(name = "siteSettings")
    private SiteSettings siteSettings;

    @MockBean
    private ApplicationSettings.SearchSettings searchSettings;

    @MockBean
    private ApplicationSettings.OrganSettings organSettings;

    @MockBean
    private ApplicationSettings.InternationalSearchSettings iSearchSettings;

    @MockBean
    private ApplicationSettings.OntologySettings ontologySettings;

    @MockBean(name = "userService")
    private UserService userService;

    @MockBean(name = "taxonService")
    private TaxonService taxonService;

    @MockBean(name = "geneService")
    private GeneInfoService geneService;

    @MockBean(name = "userGeneService")
    private UserGeneService userGeneService;

    @MockBean(name = "userOrganService")
    private UserOrganService userOrganService;

    @MockBean
    private RemoteResourceService remoteResourceService;

    @MockBean(name = "userPrivacyService")
    private UserPrivacyService privacyService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "organInfoService")
    private OrganInfoService organInfoService;

    @MockBean
    private UserListener userListener;

    @MockBean(name = "ontologyService")
    private OntologyService ontologyService;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Before
    public void setUp() {
        when( applicationSettings.getEnabledTiers() ).thenReturn( EnumSet.allOf( TierType.class ) );
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( applicationSettings.getProfile() ).thenReturn( profileSettings );
        when( profileSettings.getEnabledResearcherCategories() ).thenReturn( EnumSet.allOf( ResearcherCategory.class ) );
        when( profileSettings.getEnabledResearcherPositions() ).thenReturn( EnumSet.of( ResearcherPosition.PRINCIPAL_INVESTIGATOR ) );
        when( applicationSettings.getSearch() ).thenReturn( searchSettings );
        when( searchSettings.getEnabledSearchModes() ).thenReturn( new LinkedHashSet<>( EnumSet.allOf( ApplicationSettings.SearchSettings.SearchMode.class ) ) );
        when( applicationSettings.getOrgans() ).thenReturn( organSettings );
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( applicationSettings.getOntology() ).thenReturn( ontologySettings );
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.SEARCH ) ) ).thenReturn( true );
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.INTERNATIONAL_SEARCH ) ) ).thenReturn( true );
    }

    @Test
    public void getSearch_return200() throws Exception {
        mvc.perform( get( "/search" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) );
        verify( userService ).getLastNamesFirstChar();
        verify( userService ).findCurrentUser();
        verifyNoMoreInteractions( userService );
    }

    @Test
    public void getSearch_withoutPublicSearch_redirect3xx() throws Exception {
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.SEARCH ) ) ).thenReturn( false );
        mvc.perform( get( "/search" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "http://localhost/login" ) );
        verifyNoInteractions( userService );
    }

    @Test
    public void getSearch_ByNameLike_return200() throws Exception {
        mvc.perform( get( "/search" )
                        .param( "nameLike", "K" )
                        .param( "prefix", "true" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) )
                .andExpect( model().attributeExists( "users" ) );
        verify( userService ).findByStartsName( "K", null, null, null, null );
    }

    @Test
    public void getSearch_ByDescriptionLike_return200() throws Exception {
        mvc.perform( get( "/search" )
                        .param( "descriptionLike", "pancake" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) )
                .andExpect( model().attributeExists( "users" ) );
        verify( userService ).findByDescription( "pancake", null, null, null, null );
    }

    @Test
    public void searchByNameAndDescription_thenReturn200() throws Exception {
        mvc.perform( get( "/search" )
                        .param( "nameLike", "maple" )
                        .param( "descriptionLike", "pancake" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) )
                .andExpect( model().attributeExists( "users" ) );
        verify( userService ).findByNameAndDescription( "maple", false, "pancake", null, null, null, null );
    }

    @Test
    public void searchByNameAndDescription_whenNameIsEmpty_thenRedirectToSearchByDescription() throws Exception {
        mvc.perform( get( "/search" )
                        .param( "nameLike", "" )
                        .param( "descriptionLike", "pancake" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search" ) );
    }

    @Test
    public void searchByNameAndDescription_whenDescriptionIsEmpty_thenRedirectToSearchByName() throws Exception {
        mvc.perform( get( "/search" )
                        .param( "nameLike", "maple" )
                        .param( "descriptionLike", "" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search" ) );
    }

    @Test
    public void getSearch_ByGeneSymbol_return200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        GeneInfo gene = createGene( 1, humanTaxon );
        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "BRCA1", humanTaxon ) ).thenReturn( gene );
        mvc.perform( get( "/search" )
                        .param( "symbol", "BRCA1" )
                        .param( "taxonId", "9606" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) )
                .andExpect( model().attributeExists( "usergenes" ) );
        verify( userGeneService ).handleGeneSearch( gene, TierType.ANY, null, null, null, null, null );
    }

    @Test
    public void getSearch_whenGeneSymbolIsEmpty_returnBadRequest() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        GeneInfo gene = createGene( 1, humanTaxon );
        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "BRCA1", humanTaxon ) ).thenReturn( gene );
        mvc.perform( get( "/search" )
                        .param( "symbol", "" )
                        .param( "taxonId", "9606" )
                        .param( "iSearch", "false" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "search" ) );
    }

    @Test
    public void getUser_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( user.getId() ) ).thenReturn( user );
        mvc.perform( get( "/search/user/{userId}", user.getId() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search/user" ) );
        verify( userService ).findUserById( user.getId() );
    }

    @Test
    public void getUser_whenUserIsNotFound_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( user.getId() ) ).thenReturn( null );
        mvc.perform( get( "/search/user/{userId}", user.getId() ) )
                .andExpect( status().isNotFound() );
        verify( userService ).findUserById( user.getId() );
    }

    @Test
    public void getUser_whenUserIsRemote_thenReturnSuccess() throws Exception {
        User user = createRemoteUser( 1, URI.create( "https://example.com/" ) );
        when( remoteResourceService.getRemoteUser( user.getId(), URI.create( "example.com" ) ) ).thenReturn( remotify( user, User.class ) );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", user.getId() )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search/user" ) );
        verify( remoteResourceService ).getRemoteUser( user.getId(), URI.create( "example.com" ) );
    }

    @Test
    public void getUser_whenUserIsRemoteAndHasTaxonWithoutOrdering_thenReturnSuccess() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        Taxon mouseTaxon = createTaxon( 12145 );
        humanTaxon.setOrdering( 1 );
        mouseTaxon.setOrdering( 2 );
        Gene cdh1 = createGene( 1, humanTaxon );
        Gene brca1 = createGene( 2, mouseTaxon );
        User user = createUserWithGenes( 1, cdh1, brca1 );
        user.setOriginUrl( URI.create( "https://example.com/" ) );
        assertThat( user.getTaxons() ).hasSize( 2 );
        User remoteUser = remotify( user, User.class );
        assertThat( remoteUser.getTaxons() ).hasSize( 2 ).extracting( "ordering" ).containsOnly( (Integer) null );
        when( remoteResourceService.getRemoteUser( user.getId(), URI.create( "example.com" ) ) ).thenReturn( remoteUser );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", user.getId() )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search/user" ) )
                .andExpect( model().attribute( "viewUser", Matchers.hasProperty( "taxons",
                        Matchers.everyItem( Matchers.hasProperty( "ordering", Matchers.nullValue() ) ) ) ) );
        verify( remoteResourceService ).getRemoteUser( user.getId(), URI.create( "example.com" ) );
    }

    @Test
    public void getUser_whenRemoteUserIsNotFound_thenReturnNotFound() throws Exception {
        when( remoteResourceService.getRemoteUser( 1, URI.create( "example.com" ) ) ).thenReturn( null );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", 1 )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isNotFound() );
        verify( remoteResourceService ).getRemoteUser( 1, URI.create( "example.com" ) );
    }

    @Test
    public void getUser_whenRemoteIsUnknown_thenReturnBadRequest() throws Exception {
        when( remoteResourceService.getRemoteUser( 1, URI.create( "example.com" ) ) ).thenThrow( UnknownRemoteApiException.class );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", 1 )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isBadRequest() );
        verify( remoteResourceService ).getRemoteUser( 1, URI.create( "example.com" ) );
    }

    @Test
    public void getUser_whenRemoteIsUnavailable_thenReturnNotFound() throws Exception {
        when( remoteResourceService.getRemoteUser( 1, URI.create( "example.com" ) ) ).thenThrow( RemoteException.class );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", 1 )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isServiceUnavailable() );
        verify( remoteResourceService ).getRemoteUser( 1, URI.create( "example.com" ) );
    }

    @Test
    public void searchItlUsers_whenNameLikeIsEmpty_thenRedirectToDescriptionLikeSearch() throws Exception {
        mvc.perform( get( "/search/view/international" )
                        .param( "nameLike", "" )
                        .param( "descriptionLike", "pancake" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search/view/international" ) );
    }

    @Test
    public void searchUsersView_whenNameLikeIsEmpty_thenRedirectToDescriptionLikeSearch() throws Exception {
        mvc.perform( get( "/search/view" )
                        .param( "nameLike", "maple" )
                        .param( "descriptionLike", "" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search/view" ) );
    }

    @Test
    public void searchUsersView_whenSummarizing_thenReturnSummary() throws Exception {
        mvc.perform( get( "/search/view" )
                        .param( "nameLike", "albert" )
                        .param( "descriptionLike", "cheesecake" )
                        .param( "summarize", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_HTML ) );
    }

    @Test
    public void searchUsersByNameView_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/search/view" )
                        .param( "nameLike", "Mark" )
                        .param( "prefix", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/user-table::user-table" ) );
        verify( userService ).findByStartsName( "Mark", null, null, null, null );
    }

    @Test
    public void searchUsersByNameView_whenSearchIsUnavailable_thenReturnUnauthorized() throws Exception {
        // The frontend cannot handle 3xx redirection to the login page as that would return a full-fledged HTML
        // document, so instead it must produce a 401 Not Authorized exception
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.SEARCH ) ) ).thenReturn( false );
        mvc.perform( get( "/search/view" )
                        .param( "nameLike", "Mark" )
                        .param( "prefix", "true" ) )
                .andExpect( status().isUnauthorized() )
                .andExpect( view().name( "fragments/error::message" ) );
        verifyNoInteractions( userService );
    }

    @Test
    public void searchUsersByGeneSymbolView_whenSymbolIsEmpty_thenReturnBadRequest() throws Exception {
        when( taxonService.findById( 9606 ) ).thenReturn( createTaxon( 9606 ) );
        mvc.perform( get( "/search/view" )
                        .param( "symbol", "" )
                        .param( "taxonId", "9606" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "fragments/error::message" ) )
                .andExpect( model().attribute( "errorMessage", "Gene symbol cannot be empty." ) );
        verifyNoInteractions( userGeneService );
    }

    @Test
    public void searchItlUsersByNameView_thenReturnSuccess() throws Exception {
        User user = createRemoteUser( 1, URI.create( "http://example.com/" ) );
        when( remoteResourceService.findUsersByLikeName( "Mark", true, null, null, null, null ) )
                .thenReturn( Collections.singletonList( remotify( user, User.class ) ) );
        mvc.perform( get( "/search/view/international" )
                        .param( "nameLike", "Mark" )
                        .param( "prefix", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/user-table::user-table" ) );
        verify( remoteResourceService ).findUsersByLikeName( "Mark", true, null, null, null, null );
    }

    @Test
    public void searchItlUsersByGeneSymbol_whenGeneSymbolIsEmpty_thenReturnBadRequest() throws Exception {
        when( taxonService.findById( 9606 ) ).thenReturn( createTaxon( 9606 ) );
        mvc.perform( get( "/search/view/international" )
                        .param( "symbol", "" )
                        .param( "taxonId", "9606" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "fragments/error::message" ) )
                .andExpect( model().attribute( "errorMessage", "Gene symbol cannot be empty." ) );
        verifyNoInteractions( remoteResourceService );
    }

    @Test
    public void getUser_whenRemoteUserCannotBeRetrieved_thenReturnNotFound() throws Exception {
        User user = createRemoteUser( 1, URI.create( "https://example.com/" ) );
        when( remoteResourceService.getRemoteUser( user.getId(), URI.create( "example.com" ) ) ).thenThrow( RemoteException.class );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/search/user/{userId}", user.getId() )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( status().isServiceUnavailable() );
        verify( remoteResourceService ).getRemoteUser( user.getId(), URI.create( "example.com" ) );
    }

    @Test
    public void getUser_whenOldEndpointIsUsed_thenRedirectToNewEndpoint() throws Exception {
        User user = createUser( 1 );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/userView/{userId}", user.getId() )
                        .param( "remoteHost", "example.com" ) )
                .andExpect( redirectedUrl( "/search/user/1?remoteHost=example.com" ) );
    }

    @Test
    public void previewUser_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        user.getProfile().setDescription( "This is a description." );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        mvc.perform( get( "/search/view/user-preview/{userId}", user.getId() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/profile::user-preview" ) )
                .andExpect( model().attribute( "user", user ) );
        verify( userService ).findUserById( 1 );
    }

    @Test
    public void previewUser_whenUserProfileIsEmpty_thenReturnNoContent() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        mvc.perform( get( "/search/view/user-preview/{userId}", user.getId() ) )
                .andExpect( status().isNoContent() );
        verify( userService ).findUserById( 1 );
    }

    @Test
    public void previewUser_whenUserIsRemote_thenReturnSuccess() throws Exception {
        User user = createRemoteUser( 1, URI.create( "https://example.com/" ) );
        user.getProfile().setDescription( "This is a description." );
        when( userService.findUserById( 1 ) ).thenReturn( user );
        when( remoteResourceService.getRemoteUser( 1, URI.create( "http://localhost/" ) ) ).thenReturn( remotify( user, User.class ) );
        mvc.perform( get( "/search/view/user-preview/{userId}", user.getId() )
                        .param( "remoteHost", "http://localhost/" ) )
                .andExpect( status().isOk() );
        verify( remoteResourceService ).getRemoteUser( 1, URI.create( "http://localhost/" ) );
    }

    @Test
    public void previewAnonymousUser_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        User anonymousUser = createAnonymousUser();
        anonymousUser.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        when( userService.findUserByAnonymousIdNoAuth( anonymousUser.getAnonymousId() ) ).thenReturn( user );
        when( userService.anonymizeUser( user ) ).thenReturn( anonymousUser );
        mvc.perform( get( "/search/view/user-preview/by-anonymous-id/{anonymousId}", anonymousUser.getAnonymousId() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/profile::user-preview" ) )
                .andExpect( model().attribute( "user", anonymousUser ) );
        verify( userService ).anonymizeUser( user );
    }

    @Test
    public void previewAnonymousUser_whenUserProfileIsEmpty_thenReturnNoContent() throws Exception {
        User user = createUser( 1 );
        User anonymousUser = createAnonymousUser();
        when( userService.findUserByAnonymousIdNoAuth( anonymousUser.getAnonymousId() ) ).thenReturn( user );
        when( userService.anonymizeUser( user ) ).thenReturn( anonymousUser );
        mvc.perform( get( "/search/view/user-preview/by-anonymous-id/{anonymousId}", anonymousUser.getAnonymousId() ) )
                .andExpect( status().isNoContent() )
                .andExpect( view().name( "fragments/profile::user-preview" ) )
                .andExpect( model().attribute( "user", anonymousUser ) );
        verify( userService ).anonymizeUser( user );
    }

    @Test
    public void previewAnonymousUser_whenUserIsRemote_thenReturnSuccess() throws Exception {
        User anonymousUser = createAnonymousRemoteUser( URI.create( "http://example.com/" ) );
        anonymousUser.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        when( remoteResourceService.getApiVersion( URI.create( "http://localhost/" ) ) ).thenReturn( "1.4.0" );
        when( remoteResourceService.getAnonymizedUser( anonymousUser.getAnonymousId(), URI.create( "http://localhost/" ) ) ).thenReturn( remotify( anonymousUser, User.class ) );
        mvc.perform( get( "/search/view/user-preview/by-anonymous-id/{anonymousId}", anonymousUser.getAnonymousId() )
                        .param( "remoteHost", "http://localhost/" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/profile::user-preview" ) )
                .andExpect( model().attribute( "user", anonymousUser ) );
        verify( remoteResourceService ).getAnonymizedUser( anonymousUser.getAnonymousId(), URI.create( "http://localhost/" ) );
    }

    @Test
    public void previewAnonymousUser_whenUserIsRemoteAndApiVersionIsPre14_thenReturnNotFound() throws Exception {
        User anonymousUser = createAnonymousUser();
        anonymousUser.getProfile().getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        when( remoteResourceService.getAnonymizedUser( anonymousUser.getAnonymousId(), URI.create( "http://localhost/" ) ) )
                .thenReturn( null ); // pre-1.4 returns null when the API version is not satisfied
        mvc.perform( get( "/search/view/user-preview/by-anonymous-id/{anonymousId}", anonymousUser.getAnonymousId() )
                        .param( "remoteHost", "http://localhost/" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "fragments/error::message" ) );
        verify( remoteResourceService ).getAnonymizedUser( anonymousUser.getAnonymousId(), URI.create( "http://localhost/" ) );
        verifyNoMoreInteractions( remoteResourceService );
    }

    @Test
    @WithMockUser
    public void requestAccess_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        Taxon taxon = createTaxon( 1 );
        GeneInfo gene = createGene( 1, taxon );
        UserGene userGene = createUserGene( 1, gene, createUser( 2 ), TierType.TIER1, PrivacyLevelType.PRIVATE );
        UserGene anonymizedUserGene = UserGene.builder( User.builder( new Profile() ).build() )
                .anonymousId( UUID.randomUUID() )
                .build();
        when( userService.anonymizeUserGene( userGene ) ).thenReturn( anonymizedUserGene );
        when( userService.findUserGeneByAnonymousIdNoAuth( anonymizedUserGene.getAnonymousId() ) ).thenReturn( userGene );

        mvc.perform( get( "/search/gene/by-anonymous-id/{anonymousId}/request-access", anonymizedUserGene.getAnonymousId() ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( model().attribute( "userGene", anonymizedUserGene ) );
        verify( userService ).anonymizeUserGene( userGene );

        mvc.perform( post( "/search/gene/by-anonymous-id/{anonymousId}/request-access", anonymizedUserGene.getAnonymousId() )
                        .param( "reason", "Because." ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search" ) )
                .andExpect( flash().attributeExists( "message" ) );
        verify( userService ).sendGeneAccessRequest( user, userGene, "Because." );
    }

    @Test
    @WithMockUser
    public void requestAccess_whenUserHasPermission_thenRedirectToUserProfile() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        Taxon taxon = createTaxon( 1 );
        GeneInfo gene = createGene( 1, taxon );
        UserGene userGene = createUserGene( 1, gene, createUser( 2 ), TierType.TIER1, PrivacyLevelType.PRIVATE );
        UserGene anonymizedUserGene = UserGene.builder( User.builder( new Profile() ).build() )
                .anonymousId( UUID.randomUUID() )
                .build();
        when( userService.anonymizeUserGene( userGene ) ).thenReturn( anonymizedUserGene );
        when( userService.findUserGeneByAnonymousIdNoAuth( anonymizedUserGene.getAnonymousId() ) ).thenReturn( userGene );

        when( permissionEvaluator.hasPermission( any(), eq( userGene ), eq( Permissions.READ ) ) ).thenReturn( true );

        mvc.perform( get( "/search/gene/by-anonymous-id/{anonymousId}/request-access", anonymizedUserGene.getAnonymousId() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search/user/2" ) )
                .andExpect( flash().attributeExists( "message" ) );
    }

    @Test
    public void getOntologyAccessibility() throws Exception {
        Ontology mondo = Ontology.builder( "mondo" ).build();
        mondo.getTerms().addAll( IntStream.range( 1, 5 )
                .mapToObj( i -> OntologyTermInfo.builder( mondo, String.format( "MONDO:%06d", i ) ).name( String.format( "MONDO:%06d", i ) ).build() )
                .collect( Collectors.toList() ) );
        Ontology uberon = Ontology.builder( "uberon" ).build();
        mondo.getTerms().addAll( IntStream.range( 5, 9 )
                .mapToObj( i -> OntologyTermInfo.builder( uberon, String.format( "UBERON:%06d", i ) ).name( String.format( "UBERON:%06d", i ) ).build() )
                .collect( Collectors.toList() ) );
        when( ontologyService.findAllTermsByIdIn( any() ) )
                .thenReturn( new ArrayList<>( mondo.getTerms() ) );

        // in this scenario, a partner API has a few terms in mondo and does not have uberon altogether
        RemoteOntology remoteMondo = RemoteOntology.builder( "mondo" )
                .origin( "RDP" )
                .originUrl( URI.create( "http://example.com" ) ).build();
        when( remoteResourceService.getApiUris() ).thenReturn( Collections.singletonList( URI.create( "http://example.com" ) ) );
        when( remoteResourceService.getTermsByOntologyNameAndTerms( eq( mondo ), any(), eq( URI.create( "http://example.com" ) ) ) )
                .thenReturn( CompletableFuture.completedFuture( createRemoteOntologyTerms( remoteMondo, "MONDO:000001", "MONDO:000003", "MONDO:000004" ) ) );
        when( remoteResourceService.getTermsByOntologyNameAndTerms( eq( uberon ), any(), eq( URI.create( "http://example.com" ) ) ) )
                .thenReturn( CompletableFuture.completedFuture( null ) );

        mvc.perform( get( "/search/view/international/available-terms-by-partner" )
                        .queryParam( "ontologyTermIds", "1", "2", "3", "4", "5", "6", "7", "8" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_HTML ) )
                // make sure the registry name gets interpolated correctly
                .andExpect( content().string( Matchers.containsString( "RDP</a> does not use" ) ) )
                .andExpect( content().string( Matchers.containsString( "The following partner registries do not use the following categories and/or terms which impacts the results" ) ) )
                .andExpect( model().attribute( "ontologyAvailabilityByApiUri",
                        hasEntry( Matchers.equalTo( URI.create( "http://example.com" ) ),
                                Matchers.containsInAnyOrder(
                                        allOf(
                                                hasProperty( "origin", equalTo( "RDP" ) ),
                                                hasProperty( "originUrl", equalTo( URI.create( "http://example.com" ) ) ),
                                                hasProperty( "ontology", is( mondo ) ),
                                                hasProperty( "available", equalTo( true ) ),
                                                hasProperty( "availableTerms", Matchers.hasSize( 3 ) ),
                                                hasProperty( "missingTerms", Matchers.hasSize( 1 ) )
                                        ),
                                        allOf(
                                                // even if Uberon terms are missing from the output object, the origin and originUrl can still be inferred using MONDO
                                                hasProperty( "origin", equalTo( "RDP" ) ),
                                                hasProperty( "originUrl", equalTo( URI.create( "http://example.com" ) ) ),
                                                hasProperty( "ontology", is( uberon ) ),
                                                hasProperty( "available", equalTo( false ) ),
                                                hasProperty( "availableTerms", Matchers.empty() ),
                                                hasProperty( "missingTerms", Matchers.hasSize( 4 ) )
                                        )
                                ) ) ) );
        verify( ontologyService ).findAllTermsByIdIn( Arrays.asList( 1, 2, 3, 4, 5, 6, 7, 8 ) );
    }

    @Test
    public void getOntologyAccessibility_whenNoTermsAreSupplied() throws Exception {
        mvc.perform( get( "/search/view/international/available-terms-by-partner" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "fragments/error::message" ) );
        verifyNoInteractions( ontologyService );
        verifyNoInteractions( remoteResourceService );
    }

    @Test
    public void getOntologyAccessibility_whenNonExistingTermsAreSupplied() throws Exception {
        mvc.perform( get( "/search/view/international/available-terms-by-partner" )
                        .queryParam( "ontologyTermIds", "1" ) )
                .andExpect( status().isOk() )
                .andExpect( content().string( Matchers.not( Matchers.containsString( "There are missing categories and terms in partner registries, so not all results can be displayed." ) ) ) );
        verify( ontologyService ).findAllTermsByIdIn( Collections.singletonList( 1 ) );
        verify( remoteResourceService, VerificationModeFactory.atLeastOnce() ).getApiUris();
        verifyNoMoreInteractions( remoteResourceService );
    }

    /**
     * Emulate the behaviour of an object retrieved from a partner API.
     * <p>
     * Note: this is obviously incomplete, a better way to do this would be to inject an {@link ApiController}, but that
     * is not appropriate for unit testing. Keep in mind that varying versions of the software will produce different
     * serialization.
     */
    private <T> T remotify( Object object, Class<T> objectClass ) throws IOException {
        return objectMapper.readValue( objectMapper.writeValueAsBytes( object ), objectClass );
    }
}
