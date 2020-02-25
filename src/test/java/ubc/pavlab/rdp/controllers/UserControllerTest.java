package ubc.pavlab.rdp.controllers;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.BaseTest;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
@Import(WebSecurityConfig.class)
public class UserControllerTest extends BaseTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TaxonService taxonService;

    @MockBean
    private GeneService geneService;

    @MockBean
    private GOService goService;

    //    WebSecurityConfig
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @MockBean
    private SiteSettings siteSettings;

    @Before
    public void setUp() {
        Mockito.when( taxonService.findById( Mockito.any() ) ).then( i -> createTaxon( i.getArgumentAt( 0, Integer.class ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenGetUser_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUser_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.email", is( user.getEmail() ) ) )
                .andExpect( jsonPath( "$.password" ).doesNotExist() );
    }

    @Test
    public void givenNotLoggedIn_whenGetUserTaxons_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUserTaxons_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        user.getUserGenes().put( 1, new UserGene( createGene( 1, taxon ), user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].id", is( taxon.getId() ) ) )
                .andExpect( jsonPath( "$[0].commonName", is( taxon.getCommonName() ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenGetUserGenes_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/gene" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUserGenes_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        Gene gene = createGene( 1, taxon );
        gene.setSymbol( "bat" );
        user.getUserGenes().put( 1, new UserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/gene" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].geneId", is( gene.getGeneId() ) ) )
                .andExpect( jsonPath( "$[0].symbol", is( gene.getSymbol() ) ) )
                .andExpect( jsonPath( "$[0].tier", is( TierType.TIER1.toString() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon.getId() ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenGetUserTerms_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/term" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUserTerms_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        GeneOntologyTerm term = createTerm( toGOId( 1 ) );
        term.setName( "cave" );
        UserTerm ut = new UserTerm( term, taxon, null );
        ut.getSizesByTaxon().put( taxon, 99L );
        user.getUserTerms().add( ut );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/term" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].goId", is( term.getGoId() ) ) )
                .andExpect( jsonPath( "$[0].name", is( term.getName() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon.getId() ) ) )
                .andExpect( jsonPath( "$[0].sizesByTaxon" ).doesNotExist() );
    }

    @Test
    public void givenNotLoggedIn_whenGetUserGenesInTaxon_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon/9606/gene" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUserGenesInTaxon_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        Gene gene = createGene( 1, taxon );
        gene.setSymbol( "bat" );
        user.getUserGenes().put( 1, new UserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        Taxon taxon2 = createTaxon( 2 );
        Gene gene2 = createGene( 2, taxon2 );
        gene2.setSymbol( "bat2" );
        user.getUserGenes().put( 2, new UserGene( gene2, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon/1/gene" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].geneId", is( gene.getGeneId() ) ) )
                .andExpect( jsonPath( "$[0].symbol", is( gene.getSymbol() ) ) )
                .andExpect( jsonPath( "$[0].tier", is( TierType.TIER1.toString() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon.getId() ) ) );

        mvc.perform( get( "/user/taxon/2/gene" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].geneId", is( gene2.getGeneId() ) ) )
                .andExpect( jsonPath( "$[0].symbol", is( gene2.getSymbol() ) ) )
                .andExpect( jsonPath( "$[0].tier", is( TierType.TIER3.toString() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon2.getId() ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenGetUserTermsInTaxon_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon/9606/term" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUserTermsInTaxon_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        GeneOntologyTerm term = createTerm( toGOId( 1 ) );
        term.setName( "term name1" );
        term.setAspect( Aspect.biological_process );
        user.getUserTerms().add( new UserTerm( term, taxon, null ) );

        Taxon taxon2 = createTaxon( 2 );
        GeneOntologyTerm term2 = createTerm( toGOId( 2 ) );
        term2.setName( "term name2" );
        term2.setAspect( Aspect.molecular_function );
        user.getUserTerms().add( new UserTerm( term2, taxon2, null ) );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon/1/term" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].goId", is( term.getGoId() ) ) )
                .andExpect( jsonPath( "$[0].name", is( term.getName() ) ) )
                .andExpect( jsonPath( "$[0].definition", is( term.getDefinition() ) ) )
                .andExpect( jsonPath( "$[0].aspect", is( term.getAspect().getLabel() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon.getId() ) ) );

        mvc.perform( get( "/user/taxon/2/term" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].goId", is( term2.getGoId() ) ) )
                .andExpect( jsonPath( "$[0].name", is( term2.getName() ) ) )
                .andExpect( jsonPath( "$[0].definition", is( term2.getDefinition() ) ) )
                .andExpect( jsonPath( "$[0].aspect", is( term2.getAspect().getLabel() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id", is( taxon2.getId() ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenRecommendTerms_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( get( "/user/taxon/1/term/recommend" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenRecommendTerms_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        UserTerm t1 = new UserTerm( createTerm( toGOId( 1 ) ), taxon, null );
        UserTerm t2 = new UserTerm( createTerm( toGOId( 2 ) ), taxon, null );

        Taxon taxon2 = createTaxon( 2 );
        UserTerm t3 = new UserTerm( createTerm( toGOId( 3 ) ), taxon2, null );
        UserTerm t4 = new UserTerm( createTerm( toGOId( 4 ) ), taxon2, null );


        given( userService.findCurrentUser() ).willReturn( user );
        given( userService.recommendTerms( Mockito.any(), Mockito.eq( taxon ) ) ).willReturn( Sets.newSet( t1, t2 ) );
        given( userService.recommendTerms( Mockito.any(), Mockito.eq( taxon2 ) ) ).willReturn( Sets.newSet( t3, t4 ) );

        mvc.perform( get( "/user/taxon/1/term/recommend" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$", hasSize( 2 ) ) )
                .andExpect( jsonPath( "$[*].goId", containsInAnyOrder( t1.getGoId(), t2.getGoId() ) ) )
                .andExpect( jsonPath( "$[*].taxon.id", contains( taxon.getId(), taxon.getId() ) ) );

        mvc.perform( get( "/user/taxon/2/term/recommend" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$", hasSize( 2 ) ) )
                .andExpect( jsonPath( "$[*].goId", containsInAnyOrder( t3.getGoId(), t4.getGoId() ) ) )
                .andExpect( jsonPath( "$[*].taxon.id", contains( taxon2.getId(), taxon2.getId() ) ) );
    }

    // POST

    @Test
    public void givenNotLoggedIn_whenSaveProfile_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "" ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfile_thenReturnSucceed()
            throws Exception {

        User user = createUser( 1 );
        user.getProfile().setDepartment( "Department" );
        user.getProfile().setDescription( "Description" );
        user.getProfile().setLastName( "LastName" );
        user.getProfile().setName( "Name" );
        user.getProfile().setOrganization( "Organization" );
        user.getProfile().setPhone( "555-555-5555" );
        user.getProfile().setWebsite( "http://test.com" );


        given( userService.findCurrentUser() ).willReturn( user );
        String json = new JSONObject( user.getProfile() ).toString();

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( json ) )
                .andExpect( status().isOk() );

    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfileAndInvalidWebsite_thenReturn400()
            throws Exception {

        User user = createUser( 1 );
        user.getProfile().setWebsite( "malformed url" );


        given( userService.findCurrentUser() ).willReturn( user );
        String json = new JSONObject( user.getProfile() ).toString();

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( json ) )
                .andExpect( status().isBadRequest() );

    }

    @Test
    public void givenNotLoggedIn_whenSearchTermsInTaxon_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( post( "/user/taxon/1/term/search" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "" ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSearchTermsInTaxon_thenReturnJson()
            throws Exception {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        Mockito.when( userService.convertTerms( Mockito.any(), Mockito.eq( taxon ), Mockito.any( GeneOntologyTerm.class ) ) )
                .then( i -> new UserTerm( i.getArgumentAt( 2, GeneOntologyTerm.class ), taxon, null ) );
        Mockito.when( goService.getTerm( Mockito.any() ) )
                .then( i -> createTerm( i.getArgumentAt( 0, String.class ) ) );

        given( userService.findCurrentUser() ).willReturn( user );

        JSONArray json = new JSONArray();
        json.put( toGOId( 1 ) );
        json.put( toGOId( 2 ) );
        json.put( toGOId( 3 ) );

        mvc.perform( post( "/user/taxon/1/term/search" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( json.toString() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$['GO:0000001'].goId", is( toGOId( 1 ) ) ) )
                .andExpect( jsonPath( "$['GO:0000002'].goId", is( toGOId( 2 ) ) ) )
                .andExpect( jsonPath( "$['GO:0000003'].goId", is( toGOId( 3 ) ) ) )
                .andExpect( jsonPath( "$['GO:0000001'].taxon.id", is( taxon.getId() ) ) )
                .andExpect( jsonPath( "$['GO:0000002'].taxon.id", is( taxon.getId() ) ) )
                .andExpect( jsonPath( "$['GO:0000003'].taxon.id", is( taxon.getId() ) ) );

        mvc.perform( post( "/user/taxon/2/term/search" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( json.toString() ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$['GO:0000001']", isEmptyOrNullString() ) )
                .andExpect( jsonPath( "$['GO:0000002']", isEmptyOrNullString() ) )
                .andExpect( jsonPath( "$['GO:0000003']", isEmptyOrNullString() ) );
    }

    @Test
    public void givenNotLoggedIn_whenSaveModel_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );

        mvc.perform( post( "/user/model/1" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "" ) )
                .andExpect( status().is3xxRedirection() );
    }

}
