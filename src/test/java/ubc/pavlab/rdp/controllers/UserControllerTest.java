package ubc.pavlab.rdp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
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

    @Autowired
    ObjectMapper objectMapper;

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

        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenGetUser_thenReturnJson()
            throws Exception {

        User user = createUser( 1 );

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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
        UserTerm ut = UserTerm.createUserTerm( user, term, taxon );
        ut.getSizesByTaxonId().put( taxon.getId(), 99L );
        user.getUserTerms().add( ut );

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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
        user.getUserTerms().add( UserTerm.createUserTerm( user, term, taxon ) );

        Taxon taxon2 = createTaxon( 2 );
        GeneOntologyTerm term2 = createTerm( toGOId( 2 ) );
        term2.setName( "term name2" );
        term2.setAspect( Aspect.molecular_function );
        user.getUserTerms().add( UserTerm.createUserTerm( user, term2, taxon2 ) );

        when( userService.findCurrentUser() ).thenReturn( user );

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

        when( userService.findCurrentUser() ).thenReturn( user );

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
        UserTerm t1 = UserTerm.createUserTerm( user, createTerm( toGOId( 1 ) ), taxon );
        UserTerm t2 = UserTerm.createUserTerm( user, createTerm( toGOId( 2 ) ), taxon );

        Taxon taxon2 = createTaxon( 2 );
        UserTerm t3 = UserTerm.createUserTerm( user, createTerm( toGOId( 3 ) ), taxon2 );
        UserTerm t4 = UserTerm.createUserTerm( user, createTerm( toGOId( 4 ) ), taxon2 );


        when( userService.findCurrentUser() ).thenReturn( user );
        when( userService.recommendTerms( Mockito.any(), eq( taxon ) ) ).thenReturn( Sets.newSet( t1, t2 ) );
        when( userService.recommendTerms( Mockito.any(), eq( taxon2 ) ) ).thenReturn( Sets.newSet( t3, t4 ) );

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

        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "" ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Data
    static class ProfileWithoutOrganUberonIds {
        Profile profile;
    }

    @Data
    static class ProfileWithOrganUberonIds {
        Profile profile;
        Set<String> organUberonIds;
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfile_thenReturnSucceed()
            throws Exception {

        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );

        Profile updatedProfile = new Profile();
        updatedProfile.setDepartment( "Department" );
        updatedProfile.setDescription( "Description" );
        updatedProfile.setLastName( "LastName" );
        updatedProfile.setName( "Name" );
        updatedProfile.setOrganization( "Organization" );
        updatedProfile.setPhone( "555-555-5555" );
        updatedProfile.setWebsite( "http://test.com" );
        updatedProfile.setPublications( Sets.newSet() );
        updatedProfile.setPrivacyLevel( PrivacyLevelType.PRIVATE );

        ProfileWithoutOrganUberonIds payload = new ProfileWithoutOrganUberonIds();
        payload.setProfile( updatedProfile );

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( payload ) ) )
                .andExpect( status().isOk() );

        verify( userService ).updateUserProfileAndPublicationsAndOrgans( user, updatedProfile, updatedProfile.getPublications(), Optional.empty() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfileAndInvalidWebsite_thenReturn400()
            throws Exception {

        User user = createUser( 1 );
        user.getProfile().setWebsite( "malformed url" );

        when( userService.findCurrentUser() ).thenReturn( user );
        JSONObject profileJson = new JSONObject( user.getProfile() );
        JSONObject payload = new JSONObject();
        payload.put( "profile", profileJson );

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

        when( userService.findCurrentUser() ).thenReturn( user );
        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );

        JSONObject profileJson = new JSONObject( user.getProfile() );
        // FIXME: use jackson serializer to perform enum conversion from model
        profileJson.put( "privacyLevel", PrivacyLevelType.SHARED.ordinal() );

        JSONObject payload = new JSONObject();
        payload.put( "profile", profileJson );

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        Profile profile = user.getProfile();
        profile.setPrivacyLevel( PrivacyLevelType.SHARED );
        verify( userService ).updateUserProfileAndPublicationsAndOrgans( user, profile, profile.getPublications(), Optional.empty() );
    }

    @Test
    @WithMockUser
    @SneakyThrows
    public void givenLoggedIn_whenSaveProfileWithUberonOrganIds_thenReturnSuccess() {
        User user = createUser( 1 );
        Organ organ = createOrgan( "UBERON", "Appendage", null );

        when( userService.findCurrentUser() ).thenReturn( user );
        when( organInfoService.findByUberonIdIn( Mockito.anyCollection() ) ).thenReturn( Sets.newSet( organ ) );

        assertThat( user.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PUBLIC );

        JSONObject profileJson = new JSONObject( user.getProfile() );
        // FIXME
        profileJson.put( "privacyLevel", user.getProfile().getPrivacyLevel().ordinal() );

        JSONObject payload = new JSONObject();
        payload.put( "profile", profileJson );
        payload.put( "organUberonIds", new JSONArray( new String[]{ organ.getUberonId() } ) );

        mvc.perform( post( "/user/profile" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( payload.toString() ) )
                .andExpect( status().isOk() );

        verify( userService ).updateUserProfileAndPublicationsAndOrgans( user, user.getProfile(), user.getProfile().getPublications(), Optional.of( Sets.newSet( organ.getUberonId() ) ) );
    }

    @Test
    public void givenNotLoggedIn_whenSearchTermsInTaxon_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        when( userService.findCurrentUser() ).thenReturn( user );

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

        Mockito.when( userService.convertTerms( Mockito.any(), eq( taxon ), Mockito.anyCollectionOf( GeneOntologyTerm.class ) ) )
                .then( i -> ( (Collection<GeneOntologyTerm>) i.getArgumentAt( 2, Collection.class ) ).stream()
                        .map( goTerm -> UserTerm.createUserTerm( user, goTerm, taxon ) ).collect( Collectors.toSet() ) );
        Mockito.when( goService.getTerm( Mockito.any() ) )
                .then( i -> createTerm( i.getArgumentAt( 0, String.class ) ) );

        when( userService.findCurrentUser() ).thenReturn( user );

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
                .andExpect( content().string( "{}" ) );
    }

    @Test
    public void givenNotLoggedIn_whenSaveModel_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );

        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( post( "/user/model/1" )
                .contentType( MediaType.APPLICATION_JSON )
                .content( "" ) )
                .andExpect( status().is3xxRedirection() );
    }

}
