package ubc.pavlab.rdp.controllers;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@RunWith(SpringRunner.class)
@WebMvcTest(SearchController.class)
@Import(WebSecurityConfig.class)
public class SearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean(name = "applicationSettings")
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.ProfileSettings profileSettings;

    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;

    @MockBean(name = "siteSettings")
    private SiteSettings siteSettings;

    @MockBean
    private ApplicationSettings.OrganSettings organSettings;

    @MockBean
    private ApplicationSettings.InternationalSearchSettings iSearchSettings;

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

    @MockBean(name = "tierService")
    private TierService tierService;

    @MockBean
    private RemoteResourceService remoteResourceService;

    @MockBean
    private PrivacyService privacyService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "organInfoService")
    private OrganInfoService organInfoService;

    @Before
    public void setUp() {
        when( applicationSettings.getEnabledTiers() ).thenReturn( Lists.newArrayList( "TIER1", "TIER2", "TIER3" ) );
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( applicationSettings.getProfile() ).thenReturn( profileSettings );
        when( profileSettings.getEnabledResearcherCategories() ).thenReturn( Lists.newArrayList( "TIER1", "TIER2", "TIER3" ) );
        when( profileSettings.getEnabledResearcherPositions() ).thenReturn( Lists.newArrayList( "PRINCIPAL_INVESTIGATOR" ) );
        when( applicationSettings.getOrgans() ).thenReturn( organSettings );
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
    }

    @Test
    public void getSearch_return200() throws Exception {
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( "search" ) ) ).thenReturn( true );
        mvc.perform( get( "/search" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "search" ) );
    }

    @Test
    public void getSearch_withoutPublicSearch_redirect3xx() throws Exception {
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( "search" ) ) ).thenReturn( false );
        mvc.perform( get( "/search" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "http://localhost/login" ) );
    }

    @Test
    public void viewUser_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( user.getId() ) ).thenReturn( user );
        mvc.perform( get( "/userView/{userId}", user.getId() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "userView" ) );
        verify( userService ).findUserById( user.getId() );
    }

    @Test
    public void viewUser_whenUserIsNotFound_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserById( user.getId() ) ).thenReturn( null );
        mvc.perform( get( "/userView/{userId}", user.getId() ) )
                .andExpect( status().isNotFound() );
        verify( userService ).findUserById( user.getId() );
    }

    @Test
    public void viewUser_whenUserIsRemote_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( remoteResourceService.getRemoteUser( user.getId(), "example.com" ) ).thenReturn( user );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/userView/{userId}", user.getId() )
                .param( "remoteHost", "example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "userView" ) );
        verify( remoteResourceService ).getRemoteUser( user.getId(), "example.com" );
    }

    @Test
    public void viewUser_whenRemoteUserIsNotFound_thenReturnNotFound() throws Exception {
        when( remoteResourceService.getRemoteUser( 1, "example.com" ) ).thenReturn( null );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/userView/{userId}", 1 )
                .param( "remoteHost", "example.com" ) )
                .andExpect( status().isNotFound() );
        verify( remoteResourceService ).getRemoteUser( 1, "example.com" );
    }

    @Test
    public void viewUser_whenRemoteIsUnavailable_thenReturnNotFound() throws Exception {
        when( remoteResourceService.getRemoteUser( 1, "example.com" ) ).thenThrow( RemoteException.class );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/userView/{userId}", 1 )
                .param( "remoteHost", "example.com" ) )
                .andExpect( status().isServiceUnavailable() );
        verify( remoteResourceService ).getRemoteUser( 1, "example.com" );
    }

    @Test
    public void searchItlUsersByNameView_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( "international-search" ) ) ).thenReturn( true );
        when( remoteResourceService.findUsersByLikeName( "Mark", true, Optional.empty(), Optional.empty() ) )
                .thenReturn( Collections.singleton( user ) );
        mvc.perform( get( "/search/view/international" )
                .param( "nameLike", "Mark" )
                .param( "prefix", "true" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/user-table::user-table" ) );
    }

    @Test
    public void viewUser_whenRemoteUserCannotBeRetrieved_thenReturnNotFound() throws Exception {
        User user = createUser( 1 );
        when( remoteResourceService.getRemoteUser( user.getId(), "example.com" ) ).thenThrow( RemoteException.class );
        when( privacyService.checkCurrentUserCanSearch( true ) ).thenReturn( true );
        mvc.perform( get( "/userView/{userId}", user.getId() )
                .param( "remoteHost", "example.com" ) )
                .andExpect( status().isServiceUnavailable() );
        verify( remoteResourceService ).getRemoteUser( user.getId(), "example.com" );
    }
}
