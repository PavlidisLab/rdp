package ubc.pavlab.rdp.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.security.SecureTokenChallenge;
import ubc.pavlab.rdp.services.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.FaqSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 13/02/18.
 */
@WebMvcTest(UserController.class)
@TestPropertySource(value = "classpath:application.properties", properties = {
        "rdp.settings.faq-file=classpath:faq.properties",
        "rdp.settings.enabled-tiers=TIER1,TIER2,TIER3",
        "rdp.settings.ontology.enabled=true",
        "rdp.settings.profile.enabled-researcher-categories=IN_SILICO,IN_VITRO_BIOCHEMICAL,IN_VITRO_CELLS,IN_VITRO_STRUCTURAL,IN_VIVO,OTHER",
        "rdp.settings.profile.enabled-researcher-positions=PRINCIPAL_INVESTIGATOR"
})
@Import({ ApplicationSettings.class, SiteSettings.class, FaqSettings.class })
public class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean(name = "userService")
    private UserService userService;

    @MockBean(name = "taxonService")
    private TaxonService taxonService;

    @MockBean
    private GeneInfoService geneService;

    @MockBean
    private GOService goService;

    @MockBean
    private PrivacyService privacyService;

    @MockBean(name = "organInfoService")
    private OrganInfoService organInfoService;

    //    WebSecurityConfig
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private EmailService emailService;

    @MockBean(name = "ontologyService")
    private OntologyService ontologyService;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @MockBean
    private SecureTokenChallenge<HttpServletRequest> secureTokenChallenge;

    @BeforeEach
    public void setUp() {
        when( taxonService.findById( any() ) ).then( i -> createTaxon( i.getArgument( 0, Integer.class ) ) );
        when( userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( any(), any(), any(), any(), any(), any(), any() ) ).thenAnswer( arg -> arg.getArgument( 0, User.class ) );
    }

    @Test
    @WithMockUser
    public void getProfile_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        mvc.perform( get( "/user/profile" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/profile" ) );
    }

    @Test
    public void getProfile_withoutUser_thenReturn3xx() throws Exception {
        mvc.perform( get( "/user/profile" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "http://localhost/login" ) );
    }

    @Test
    @WithMockUser
    public void getModel_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        mvc.perform( get( "/user/model/{taxonId}", 9606 ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/model" ) );
    }

    @Test
    @WithMockUser
    public void getTermsGenesForTaxon_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        when( goService.getTerm( any() ) ).then( i -> createTerm( i.getArgument( 0, String.class ) ) );
        mvc.perform( get( "/user/taxon/{taxonId}/term/{goId}/gene/view", 9606, "GO:0000001" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "fragments/gene-table::gene-table" ) );
    }

    @Test
    @WithMockUser
    public void getUserStaticEndpoints_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        mvc.perform( get( "/user/home" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/home" ) );
        mvc.perform( get( "/user/documentation" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/documentation" ) );
        mvc.perform( get( "/user/faq" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/faq" ) );
    }

    @Autowired
    private ApplicationSettings applicationSettings;

    @Nested
    public class WithoutFaq {

        private Resource originalFaq;

        @BeforeEach
        public void setUp() {
            originalFaq = applicationSettings.getFaqFile();
            applicationSettings.setFaqFile( null );
        }

        @AfterEach
        public void tearDown() {
            applicationSettings.setFaqFile( originalFaq );
        }

        @Test
        @WithMockUser
        public void getUserFaq_whenFaqIsDisabled_thenReturn404() throws Exception {
            mvc.perform( get( "/user/faq" ) )
                    .andExpect( status().isNotFound() )
                    .andExpect( view().name( "error/404" ) );
        }
    }

    @Test
    @WithMockUser
    public void contactSupport_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        user.setEmail( "johndoe@example.com" );
        user.getProfile().setName( "John" );
        user.getProfile().setLastName( "Doe" );
        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user/support" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/support" ) )
                .andExpect( model().attributeExists( "supportForm" ) )
                .andExpect( model().attribute( "supportForm", Matchers.hasProperty( "name", Matchers.equalTo( "Doe, John" ) ) ) );

        byte[] attachmentContent = new byte[1];
        MockMultipartFile attachment = new MockMultipartFile( "attachment", "README.md", "text/plain", attachmentContent );

        mvc.perform( multipart( "/user/support" )
                        .file( attachment )
                        .param( "name", "John Doe" )
                        .param( "message", "Is everything okay?" )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/support" ) );

        verify( emailService ).sendSupportMessage( eq( "Is everything okay?" ), eq( "John Doe" ), eq( user ), any(), eq( attachment ), eq( Locale.ENGLISH ) );
    }

    @Test
    @WithMockUser
    public void contactSupport_withUnsupportedMediaType_thenReturnBadRequest() throws Exception {
        User user = createUser( 1 );
        user.setEmail( "johndoe@example.com" );
        user.getProfile().setName( "John" );
        user.getProfile().setLastName( "Doe" );
        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user/support" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/support" ) );

        byte[] attachmentContent = new byte[1];
        MockMultipartFile attachment = new MockMultipartFile( "attachment", "blob.bin", "application/octet-stream", attachmentContent );

        mvc.perform( multipart( "/user/support" )
                        .file( attachment )
                        .param( "name", "John Doe" )
                        .param( "message", "Is everything okay?" )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "user/support" ) )
                .andExpect( model().attributeHasFieldErrors( "supportForm", "attachment" ) );

        verifyNoInteractions( emailService );
    }

    @Test
    @WithMockUser
    public void contactSupport_withEmptyAttachment_thenIgnoreAttachment() throws Exception {
        User user = createUser( 1 );
        user.setEmail( "johndoe@example.com" );
        user.getProfile().setName( "John" );
        user.getProfile().setLastName( "Doe" );
        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user/support" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/support" ) );

        byte[] attachmentContent = new byte[0];
        MockMultipartFile attachment = new MockMultipartFile( "attachment", attachmentContent );

        mvc.perform( multipart( "/user/support" )
                        .file( attachment )
                        .param( "name", "John Doe" )
                        .param( "message", "Is everything okay?" )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "user/support" ) );

        verify( emailService ).sendSupportMessage( eq( "Is everything okay?" ), eq( "John Doe" ), eq( user ), any(), isNull(), eq( Locale.ENGLISH ) );
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
                .andExpect( jsonPath( "$.email" ).value( user.getEmail() ) )
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
                .andExpect( jsonPath( "$[0].id" ).value( taxon.getId() ) )
                .andExpect( jsonPath( "$[0].commonName" ).value( taxon.getCommonName() ) );
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
                .andExpect( jsonPath( "$[0].geneId" ).value( gene.getGeneId() ) )
                .andExpect( jsonPath( "$[0].symbol" ).value( gene.getSymbol() ) )
                .andExpect( jsonPath( "$[0].tier" ).value( TierType.TIER1.toString() ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon.getId() ) );
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
        user.getUserTerms().add( ut );

        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user/term" )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].goId" ).value( term.getGoId() ) )
                .andExpect( jsonPath( "$[0].name" ).value( term.getName() ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon.getId() ) )
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
                .andExpect( jsonPath( "$[0].geneId" ).value( gene.getGeneId() ) )
                .andExpect( jsonPath( "$[0].symbol" ).value( gene.getSymbol() ) )
                .andExpect( jsonPath( "$[0].tier" ).value( TierType.TIER1.toString() ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon.getId() ) );

        mvc.perform( get( "/user/taxon/2/gene" )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].geneId" ).value( gene2.getGeneId() ) )
                .andExpect( jsonPath( "$[0].symbol" ).value( gene2.getSymbol() ) )
                .andExpect( jsonPath( "$[0].tier" ).value( TierType.TIER3.toString() ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon2.getId() ) );
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
                .andExpect( jsonPath( "$[0].goId" ).value( term.getGoId() ) )
                .andExpect( jsonPath( "$[0].name" ).value( term.getName() ) )
                .andExpect( jsonPath( "$[0].definition" ).value( term.getDefinition() ) )
                .andExpect( jsonPath( "$[0].aspect" ).value( ( term.getAspect().getLabel() ) ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon.getId() ) );

        mvc.perform( get( "/user/taxon/2/term" )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$[0].goId" ).value( term2.getGoId() ) )
                .andExpect( jsonPath( "$[0].name" ).value( term2.getName() ) )
                .andExpect( jsonPath( "$[0].definition" ).value( term2.getDefinition() ) )
                .andExpect( jsonPath( "$[0].aspect" ).value( term2.getAspect().getLabel() ) )
                .andExpect( jsonPath( "$[0].taxon.id" ).value( taxon2.getId() ) );
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
        when( userService.recommendTerms( any(), eq( taxon ), any() ) ).thenReturn( Sets.newSet( t1, t2 ) );
        when( userService.recommendTerms( any(), eq( taxon2 ), any() ) ).thenReturn( Sets.newSet( t3, t4 ) );

        mvc.perform( get( "/user/taxon/1/term/recommend" )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.recommendedTerms", hasSize( 2 ) ) )
                .andExpect( jsonPath( "$.recommendedTerms[*].goId" ).value( containsInAnyOrder( t1.getGoId(), t2.getGoId() ) ) )
                .andExpect( jsonPath( "$.recommendedTerms[*].taxon.id" ).value( contains( taxon.getId(), taxon.getId() ) ) )
                .andExpect( jsonPath( "$.feedback" ).value( nullValue() ) );
        verify( userService ).recommendTerms( eq( user ), eq( taxon ), any() );

        mvc.perform( get( "/user/taxon/2/term/recommend" )
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.recommendedTerms" ).value( hasSize( 2 ) ) )
                .andExpect( jsonPath( "$.recommendedTerms[*].goId" ).value( containsInAnyOrder( t3.getGoId(), t4.getGoId() ) ) )
                .andExpect( jsonPath( "$.recommendedTerms[*].taxon.id" ).value( contains( taxon2.getId(), taxon2.getId() ) ) )
                .andExpect( jsonPath( "$.feedback" ).value( nullValue() ) );
        verify( userService ).recommendTerms( eq( user ), eq( taxon2 ), any() );
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

        JSONObject payload = new JSONObject();
        payload.put( "department", "Department" );
        payload.put( "description", "Description" );
        payload.put( "lastName", "LastName" );
        payload.put( "name", "Name" );
        payload.put( "organization", "Organization" );
        payload.put( "phone", "555-555-5555" );
        payload.put( "website", "http://test.com" );
        payload.put( "privacyLevel", PrivacyLevelType.PRIVATE.ordinal() );
        payload.put( "publications", new ArrayList<>() );
        JSONObject payload2 = new JSONObject();
        payload2.put( "profile", payload );

        mvc.perform( post( "/user/profile" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( payload2.toString() )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isOk() );

        Profile expectedProfile = new Profile();
        expectedProfile.setDepartment( "Department" );
        expectedProfile.setDescription( "Description" );
        expectedProfile.setLastName( "LastName" );
        expectedProfile.setName( "Name" );
        expectedProfile.setOrganization( "Organization" );
        expectedProfile.setPhone( "555-555-5555" );
        expectedProfile.setWebsite( "http://test.com" );
        expectedProfile.setPrivacyLevel( PrivacyLevelType.PRIVATE );

        verify( userService ).updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, expectedProfile, expectedProfile.getResearcherCategories(), expectedProfile.getPublications(), null, null, Locale.ENGLISH );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenSaveProfileAndInvalidWebsite_thenReturn400()
            throws Exception {

        User user = createUser( 1 );
        user.getProfile().setWebsite( "malformed url" );

        when( userService.findCurrentUser() ).thenReturn( user );
        JSONObject profileJson = new JSONObject( user.getProfile() );
        profileJson.put( "privacyLevel", user.getProfile().getPrivacyLevel().ordinal() );
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
                        .content( payload.toString() )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isOk() );

        Profile profile = user.getProfile();
        profile.setPrivacyLevel( PrivacyLevelType.SHARED );
        verify( userService ).updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, profile.getResearcherCategories(), profile.getPublications(), null, null, Locale.ENGLISH );
    }

    @Test
    @WithMockUser
    @SneakyThrows
    public void givenLoggedIn_whenSaveProfileWithUberonOrganIds_thenReturnSuccess() {
        User user = createUser( 1 );
        OrganInfo organ = createOrgan( "UBERON", "Appendage", null );

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
                        .content( payload.toString() )
                        .locale( Locale.ENGLISH ) )
                .andExpect( status().isOk() );

        verify( userService ).updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, user.getProfile(), user.getProfile().getResearcherCategories(), user.getProfile().getPublications(), Sets.newSet( organ.getUberonId() ), null, Locale.ENGLISH );
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

        when( userService.convertTerm( any(), eq( taxon ), Mockito.any( GeneOntologyTermInfo.class ) ) )
                .thenAnswer( answer -> createUserTerm( 1, user, answer.getArgument( 2, GeneOntologyTerm.class ), taxon ) );
        when( goService.getTerm( any() ) )
                .then( i -> createTerm( i.getArgument( 0, String.class ) ) );

        when( userService.findCurrentUser() ).thenReturn( user );

        mvc.perform( get( "/user/taxon/1/term/search" )
                        .param( "goIds", toGOId( 1 ) )
                        .param( "goIds", toGOId( 2 ) )
                        .param( "goIds", toGOId( 3 ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$['GO:0000001'].goId" ).value( toGOId( 1 ) ) )
                .andExpect( jsonPath( "$['GO:0000002'].goId" ).value( toGOId( 2 ) ) )
                .andExpect( jsonPath( "$['GO:0000003'].goId" ).value( toGOId( 3 ) ) )
                .andExpect( jsonPath( "$['GO:0000001'].taxon.id" ).value( taxon.getId() ) )
                .andExpect( jsonPath( "$['GO:0000002'].taxon.id" ).value( taxon.getId() ) )
                .andExpect( jsonPath( "$['GO:0000003'].taxon.id" ).value( taxon.getId() ) );

        mvc.perform( get( "/user/taxon/2/term/search" )
                        .param( "goIds", toGOId( 1 ) )
                        .param( "goIds", toGOId( 2 ) )
                        .param( "goIds", toGOId( 3 ) ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$['GO:0000001']" ).isEmpty() )
                .andExpect( jsonPath( "$['GO:0000002']" ).isEmpty() )
                .andExpect( jsonPath( "$['GO:0000003']" ).isEmpty() );
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

    @Test
    @WithMockUser
    public void givenLoggedIn_whenVerifyContactEmailWithToken_thenReturn3xx() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        mvc.perform( get( "/user/verify-contact-email" ).param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/user/profile" ) )
                .andExpect( flash().attributeExists( "message" ) );
        verify( userService ).confirmVerificationToken( eq( "1234" ), any() );
        verifyNoMoreInteractions( userService );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenVerifyContactEmailWithToken_andTokenDoesNotExist_thenReturn3xx() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        when( userService.confirmVerificationToken( eq( "1234" ), any() ) ).thenAnswer( answer -> {
            throw new TokenException( "Verification token does not exist." );
        } );
        mvc.perform( get( "/user/verify-contact-email" )
                        .param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( flash().attributeExists( "message" ) )
                .andExpect( flash().attribute( "error", true ) );
        verify( userService ).confirmVerificationToken( eq( "1234" ), any() );
    }

    @Test
    @WithMockUser
    public void givenLoggedIn_whenResendContactEmailVerification_thenRedirect3xx() throws Exception {
        User user = createUser( 1 );
        VerificationToken token = createContactEmailVerificationToken( user, "1234" );
        when( userService.findCurrentUser() ).thenReturn( user );
        when( userService.createContactEmailVerificationTokenForUser( user, Locale.getDefault() ) ).thenReturn( token );
        mvc.perform( post( "/user/resend-contact-email-verification" )
                        .locale( Locale.getDefault() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/user/profile" ) )
                .andExpect( flash().attributeExists( "message" ) );
        verify( userService ).createContactEmailVerificationTokenForUser( user, Locale.getDefault() );
    }

    @Test
    @WithMockUser
    public void saveProfile_whenInvalidUrlIsProvided_thenReturnBadRequestWithMeaningfulValidationMessage() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        Profile updatedProfile = new Profile();
        updatedProfile.setWebsite( "bad-url" );
        UserController.ProfileWithOrganUberonIdsAndOntologyTerms profileWithOrganUberonIdsAndOntologyTerms = UserController.ProfileWithOrganUberonIdsAndOntologyTerms.builder().profile( updatedProfile ).build();
        mvc.perform( post( "/user/profile" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( objectMapper.writeValueAsString( profileWithOrganUberonIdsAndOntologyTerms ) ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.fieldErrors[0].field" ).value( "profile.website" ) )
                .andExpect( jsonPath( "$.fieldErrors[0].rejectedValue" ).value( "bad-url" ) );
    }

    @Test
    public void userSerialization() throws JsonProcessingException {
        User user = createUser( 1 );
        user.getProfile().setName( "John" );
        user.getProfile().setLastName( "Doe" );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PRIVATE );
        User deserializedUser = objectMapper.readValue( objectMapper.writeValueAsString( user ), User.class );
        assertThat( deserializedUser.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
        assertThat( deserializedUser.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
        assertThat( deserializedUser.getProfile().getFullName() ).isEqualTo( "Doe, John" );
        assertThat( deserializedUser.getProfile().getName() ).isEqualTo( "John" );
        assertThat( deserializedUser.getProfile().getLastName() ).isEqualTo( "Doe" );
    }

    @Test
    public void userSerialization_withNullProfile_thenDefaultToPrivate() throws JsonProcessingException {
        User user = createUser( 1 );
        user.setProfile( null );
        User deserializedUser = objectMapper.readValue( objectMapper.writeValueAsString( user ), User.class );
        assertThat( deserializedUser.getProfile().getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
        assertThat( deserializedUser.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
    }

    @Test
    public void userGeneSerialization() throws JsonProcessingException {
        User user = createUser( 1 );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PRIVATE );
        UserGene gene = UserGene.builder( user ).privacyLevel( PrivacyLevelType.PUBLIC ).build();
        UserGene deserializedGene = objectMapper.readValue( objectMapper.writeValueAsString( gene ), UserGene.class );
        assertThat( deserializedGene.getPrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
        assertThat( deserializedGene.getEffectivePrivacyLevel() ).isEqualTo( PrivacyLevelType.PRIVATE );
    }
}
