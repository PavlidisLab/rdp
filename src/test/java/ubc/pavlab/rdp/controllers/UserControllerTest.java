package ubc.pavlab.rdp.controllers;

import lombok.SneakyThrows;
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
import org.springframework.security.access.PermissionEvaluator;
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
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.SiteSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
@Import(WebSecurityConfig.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TaxonService taxonService;

    @MockBean
    private GeneInfoService geneService;

    @MockBean
    private GOService goService;

    @MockBean
    private PrivacyService privacyService;

    @MockBean
    private OrganInfoService organInfoService;

    //    WebSecurityConfig
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @MockBean
    private SiteSettings siteSettings;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

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
        user.getUserGenes().put( 1, UserGene.createUserGeneFromGene( createGene( 1, taxon ), user, TierType.TIER1, PrivacyLevelType.PUBLIC ) );

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
        user.getUserGenes().put( 1, UserGene.createUserGeneFromGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

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
        UserTerm ut = UserTerm.createUserTerm( user, term, taxon, null );
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
        user.getUserGenes().put( 1, UserGene.createUserGeneFromGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        Taxon taxon2 = createTaxon( 2 );
        Gene gene2 = createGene( 2, taxon2 );
        gene2.setSymbol( "bat2" );
        user.getUserGenes().put( 2, UserGene.createUserGeneFromGene( gene2, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

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
        user.getUserTerms().add( UserTerm.createUserTerm( user, term, taxon, null ) );

        Taxon taxon2 = createTaxon( 2 );
        GeneOntologyTerm term2 = createTerm( toGOId( 2 ) );
        term2.setName( "term name2" );
        term2.setAspect( Aspect.molecular_function );
        user.getUserTerms().add( UserTerm.createUserTerm( user, term2, taxon2, null ) );

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
        UserTerm t1 = UserTerm.createUserTerm( user, createTerm( toGOId( 1 ) ), taxon, null );
        UserTerm t2 = UserTerm.createUserTerm( user, createTerm( toGOId( 2 ) ), taxon, null );

        Taxon taxon2 = createTaxon( 2 );
        UserTerm t3 = UserTerm.createUserTerm( user, createTerm( toGOId( 3 ) ), taxon2, null );
        UserTerm t4 = UserTerm.createUserTerm( user, createTerm( toGOId( 4 ) ), taxon2, null );


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
        given( userService.findCurrentUser() ).willReturn( user );

        JSONObject updatedProfile = new JSONObject( user.getProfile() );
        updatedProfile.put("department", "Department" );
        updatedProfile.put("description", "Description" );
        updatedProfile.put("lastName",  "LastName" );
        updatedProfile.put("name", "Name" );
        updatedProfile.put("organization", "Organization" );
        updatedProfile.put("phone", "555-555-5555" );
        updatedProfile.put("website", "http://test.com" );
        updatedProfile.put("privacyLevel", PrivacyLevelType.PRIVATE.ordinal() );

        JSONObject payload = new JSONObject();
        payload.put("profile", updatedProfile);

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        assertThat( user.getProfile().getDepartment() ).isEqualTo( "Department");
        assertThat( user.getProfile().getDescription() ).isEqualTo( "Description");
        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfileAndInvalidWebsite_thenReturn400()
            throws Exception {

        User user = createUser( 1 );
        user.getProfile().setWebsite( "malformed url" );

        given( userService.findCurrentUser() ).willReturn( user );
        JSONObject profileJson = new JSONObject( user.getProfile() );
        JSONObject payload = new JSONObject();
        payload.put("profile", profileJson);

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isBadRequest() );

    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfileWithNewPrivacyLevel_thenReturn200()
            throws Exception {

        User user = createUser( 1 );

        given( userService.findCurrentUser() ).willReturn( user );
        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );

        JSONObject profileJson = new JSONObject( user.getProfile() );
        // FIXME: use jackson serializer to perform enum conversion from model
        profileJson.put("privacyLevel",  PrivacyLevelType.SHARED.ordinal());

        JSONObject payload = new JSONObject();
        payload.put("profile", profileJson);

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.SHARED );
    }

    @Test
    @WithMockUser
    @SneakyThrows
    public void givenLoggedIn_whenSaveProfileWithUberonOrganIds_thenReturnSuccess() {
        User user = createUser( 1 );
        Organ organ = createOrgan( "UBERON", "Appendage", null );

        given( userService.findCurrentUser() ).willReturn( user );
        given( organInfoService.findByUberonIdIn( Mockito.anyCollection() ) ).willReturn( Sets.newSet( organ ) );

        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );

        JSONObject profileJson = new JSONObject( user.getProfile() );
        // FIXME
        profileJson.put( "privacyLevel", user.getProfile().getPrivacyLevel().ordinal() );
        profileJson.put( "uberonOrganIds", new JSONArray( new String[]{ organ.getUberonId() } ) );

        JSONObject payload = new JSONObject();
        payload.put("profile", profileJson);

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        assertThat( user.getUserOrgans() )
                .containsKey( organ.getUberonId() )
                .containsValue( createUserOrgan( user, organ ) );

        given( organInfoService.findByUberonIdIn( Mockito.anyCollection() ) ).willReturn( Sets.newSet() );

        // clear any existing values
        payload.put( "uberonOrganIds", new JSONArray() );
        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        assertThat( user.getUserOrgans() ).isEmpty();
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
                .then( i -> UserTerm.createUserTerm( user, i.getArgumentAt( 2, GeneOntologyTerm.class ), taxon, null ) );
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
