package ubc.pavlab.rdp.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Collections;
import java.util.EnumSet;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ubc.pavlab.rdp.util.TestUtils.*;

@WebMvcTest(ApiController.class)
@RunWith(SpringRunner.class)
@Import(WebSecurityConfig.class)
public class ApiControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    MessageSource messageSource;
    @MockBean
    private UserService userService;
    @MockBean
    private TaxonService taxonService;
    @MockBean
    private GeneInfoService geneService;
    @MockBean
    TierService tierService;
    @MockBean
    private UserOrganService userOrganService;
    @MockBean
    UserGeneService userGeneService;
    @MockBean
    OrganInfoService organInfoService;
    @MockBean
    private ApplicationSettings applicationSettings;
    @MockBean
    private ApplicationSettings.InternationalSearchSettings isearchSettings;
    @MockBean
    private SiteSettings siteSettings;
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Before
    public void setUp() {
        when( applicationSettings.getIsearch() ).thenReturn( isearchSettings );
        when( isearchSettings.isEnabled() ).thenReturn( true );
    }

    @Test
    public void searchGenes_withSearchDisabled_thenReturnServiceUnavailable() throws Exception {
        when( isearchSettings.isEnabled() ).thenReturn( false );
        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1" )
                .param( "auth", "1234" ) )
                .andExpect( status().isServiceUnavailable() );
    }

    @Test
    public void searchGenes_withAuthToken_thenReturnSuccess() throws Exception {
        // configure remote authentication
        when( isearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        when( userService.getRemoteAdmin() ).thenReturn( createUser( 1 ) );

        // configure one search result
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 2 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1" )
                .param( "auth", "1234" ) )
                .andExpect( status().is2xxSuccessful() );

        verify( userService ).getRemoteAdmin();
    }

    @Test
    public void searchGenes_withInvalidAuthToken_thenReturnUnauthorized() throws Exception {
        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1" )
                .param( "auth", "unknownToken" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    public void searchGenes_whenMisconfiguredRemoteAdmin_thenReturnServiceUnavailable() throws Exception {
        when( isearchSettings.getAuthTokens() ).thenReturn( Collections.singletonList( "1234" ) );
        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1" )
                .param( "auth", "1234" ) )
                .andExpect( status().isServiceUnavailable() );
    }

    @Test
    public void searchGenes_withSingleTier_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), null, null, null );
    }

    @Test
    public void searchGenes_withMultipleTiers_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tiers", "TIER1" )
                .param( "tiers", "TIER2" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), null, null, null );
    }

    @Test
    public void searchGenes_withMultipleTiersInludingTier3_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tiers", "TIER1" )
                .param( "tiers", "TIER2" )
                .param( "tiers", "TIER3" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), null, null, null );
    }

    @Test
    public void searchGenes_withNoTiers_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1, TierType.TIER2 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null );
    }

    @Test
    public void searchGenes_withTier12_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER1_2" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null );
    }

    @Test
    public void searchGenes_withTierAny_thenReturn200() throws Exception {
        Taxon humanTaxon = createTaxon( 9606 );
        User user = createUser( 1 );
        GeneInfo cdh1GeneInfo = createGene( 1, humanTaxon );
        UserGene cdh1UserGene = createUserGene( 1, cdh1GeneInfo, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( taxonService.findById( 9606 ) ).thenReturn( humanTaxon );
        when( geneService.findBySymbolAndTaxon( "CDH1", humanTaxon ) ).thenReturn( cdh1GeneInfo );
        when( userGeneService.handleGeneSearch( cdh1GeneInfo, EnumSet.of( TierType.TIER1 ), humanTaxon, null, null ) )
                .thenReturn( Sets.newSet( cdh1UserGene ) );

        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER_ANY" ) )
                .andExpect( status().is2xxSuccessful() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );

        verify( userGeneService ).handleGeneSearch( cdh1GeneInfo, TierType.MANUAL, null, null, null );
    }

    @Test
    public void searchGenes_withInvalidTier_thenReturnBadRequest() throws Exception {
        mvc.perform( get( "/api/genes/search" )
                .param( "symbol", "CDH1" )
                .param( "taxonId", "9606" )
                .param( "tier", "TIER4" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) );
    }
}
