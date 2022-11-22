package ubc.pavlab.rdp.services;

import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.WebMvcConfig;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.events.OnRequestAccessEvent;
import ubc.pavlab.rdp.events.OnUserPasswordResetEvent;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.listeners.UserListener;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import javax.annotation.PostConstruct;
import javax.validation.ValidationException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubc.pavlab.rdp.util.TestUtils.*;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@Import(WebMvcConfig.class)
public class UserServiceImplTest {

    @TestConfiguration
    static class UserServiceImplTestContextConfiguration {

        @MockBean
        private ApplicationSettings applicationSettings;
        @MockBean
        private ApplicationSettings.InternationalSearchSettings internationalSearchSettings;

        @PostConstruct
        public void init() {
            // this needs to be set early because it is used in UserServiceImpl.afterPropertiesSet()
            when( internationalSearchSettings.getUserId() ).thenReturn( null );
            when( applicationSettings.getIsearch() ).thenReturn( internationalSearchSettings );
        }

        @Bean
        public PrivacyService privacyService() {
            return new PrivacyServiceImpl();
        }

        @Bean
        public UserServiceImpl userService() {
            return new UserServiceImpl();
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    UserServiceImpl.USERS_BY_ANONYMOUS_ID_CACHE_NAME,
                    UserServiceImpl.USER_GENES_BY_ANONYMOUS_ID_CACHE_NAME );
        }

        @Bean
        public SecureRandom secureRandom() throws NoSuchAlgorithmException {
            return SecureRandom.getInstanceStrong();
        }
    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private UserServiceImpl userService;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private GeneInfoService geneInfoService;
    @MockBean
    private UserGeneRepository userGeneRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private RoleRepository roleRepository;
    @MockBean
    private AccessTokenRepository accessTokenRepository;
    @MockBean
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean
    private VerificationTokenRepository tokenRepository;
    @MockBean
    private GOService goService;
    @MockBean
    private OrganInfoService organInfoService;
    @MockBean
    private UserListener userListener;
    @Autowired
    private ApplicationSettings applicationSettings;
    @MockBean
    private ApplicationSettings.OrganSettings organSettings;
    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;
    @MockBean
    private ApplicationSettings.ProfileSettings profileSettings;
    @MockBean
    private SiteSettings siteSettings;
    @MockBean
    private OntologyService ontologyService;
    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;
    @MockBean
    private PermissionEvaluator permissionEvaluator;


    @Before
    public void setUp() {
        User user = createUser( 1 );

        when( userRepository.findById( user.getId() ) ).thenReturn( Optional.of( user ) );
        when( userRepository.findOneWithRoles( user.getId() ) ).thenReturn( Optional.of( user ) );
        when( userRepository.save( any( User.class ) ) ).then( i -> i.getArgument( 0, User.class ) );
        when( passwordResetTokenRepository.save( any( PasswordResetToken.class ) ) ).then( i -> i.getArgument( 0, PasswordResetToken.class ) );
        when( tokenRepository.save( any( VerificationToken.class ) ) ).then( i -> i.getArgument( 0, VerificationToken.class ) );

        when( applicationSettings.getGoTermSizeLimit() ).thenReturn( 100L );
        when( applicationSettings.getOrgans() ).thenReturn( organSettings );
        when( organSettings.isEnabled() ).thenReturn( true );
        when( privacySettings.isCustomizableLevel() ).thenReturn( true );
        when( privacySettings.getEnabledLevels() ).thenReturn( EnumSet.allOf( PrivacyLevelType.class ) );
        when( privacySettings.isCustomizableGeneLevel() ).thenReturn( true );
        when( privacySettings.getEnabledGeneLevels() ).thenReturn( EnumSet.allOf( PrivacyLevelType.class ) );
        when( profileSettings.getEnabledResearcherPositions() ).thenReturn( EnumSet.allOf( ResearcherPosition.class ) );
        when( profileSettings.getEnabledResearcherCategories() ).thenReturn( EnumSet.allOf( ResearcherCategory.class ) );
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( applicationSettings.getProfile() ).thenReturn( profileSettings );

        when( geneInfoService.load( anyCollection() ) ).thenAnswer(
                a -> a.getArgument( 0, Collection.class ).stream()
                        .map( o -> geneInfoService.load( (Integer) o ) )
                        .filter( Objects::nonNull )
                        .collect( Collectors.toSet() ) );
    }

    private void setUpRoleMocks() {
        when( roleRepository.findByRole( "ROLE_USER" ) ).thenReturn( createRole( 2, "ROLE_USER" ) );
    }

    private void setUpPasswordResetTokenMocks() {
        User user = createUser( 1 );
        User otherUser = createUser( 2 );
        PasswordResetToken token = new PasswordResetToken();
        token.setUser( user );
        token.updateToken( "token1" );
        when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new PasswordResetToken();
        token.setUser( user );
        token.setToken( "token1Expired" );
        token.setExpiryDate( Instant.now().minusSeconds( 1 ) );
        when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new PasswordResetToken();
        token.setUser( otherUser );
        token.updateToken( "token2" );
        when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        when( passwordResetTokenRepository.findByToken( "tokenBad" ) ).thenReturn( null );
    }

    private void setUpVerificationTokenMocks() {
        User user = createUser( 1 );
        VerificationToken token = new VerificationToken();
        token.setUser( user );
        token.setEmail( user.getEmail() );
        token.updateToken( "token1" );
        when( tokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new VerificationToken();
        token.setUser( user );
        token.setEmail( user.getEmail() );
        token.setToken( "token1Expired" );
        token.setExpiryDate( Instant.now().minus( 1, ChronoUnit.SECONDS ) );
        when( tokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        when( tokenRepository.findByToken( "tokenBad" ) ).thenReturn( null );
    }

    private void setUpRecommendTermsMocks() {
        Map<GeneOntologyTermInfo, Long> termFrequencies = new HashMap<>();
        Taxon taxon = createTaxon( 1 );

        termFrequencies.put( createTerm( toGOId( 0 ) ), 2L );
        termFrequencies.put( createTerm( toGOId( 1 ) ), 3L );
        termFrequencies.put( createTerm( toGOId( 2 ) ), 1L );
        termFrequencies.put( createTerm( toGOId( 3 ) ), 1L );
        termFrequencies.put( createTerm( toGOId( 4 ) ), 2L );
        termFrequencies.put( createTerm( toGOId( 5 ) ), 1L );
        termFrequencies.put( createTerm( toGOId( 6 ) ), 2L );
        termFrequencies.put( createTerm( toGOId( 7 ) ), 3L );
        termFrequencies.put( createTerm( toGOId( 8 ) ), 3L );

        // These will make sure we remove redundant terms
        GeneOntologyTermInfo t98 = createTerm( toGOId( 98 ) );
        GeneOntologyTermInfo t99 = createTerm( toGOId( 99 ) );
        termFrequencies.put( t98, 1L );
        termFrequencies.put( t99, 1L );
        when( goService.getDescendants( t98 ) ).thenReturn( Collections.singleton( t99 ) );

        termFrequencies.forEach( ( key, value ) -> when( goService.getSizeInTaxon( key, taxon ) ).thenReturn( value + 9 ) );

        Map<String, GeneOntologyTermInfo> termMap = termFrequencies.keySet().stream()
                .collect( Collectors.toMap( GeneOntologyTerm::getGoId, Function.identity() ) );

        when( goService.count() ).thenReturn( (long) termMap.size() );
        when( goService.getTerm( any() ) ).thenAnswer( a -> termMap.get( a.getArgument( 0, String.class ) ) );
        when( goService.termFrequencyMap( Mockito.anyCollection() ) ).thenReturn( termFrequencies );
    }

    @Test
    public void create_whenValidUser_thenPasswordEncodedAndRoleAssigned() {
        setUpRoleMocks();

        User user = createUser( 1 );
        String oldPassword = "imbatman";
        user.setPassword( oldPassword ); // unencode

        User persistedUser = userService.create( user );

        assertThat( persistedUser.getEmail() ).isEqualTo( user.getEmail() );
        Role role = new Role();
        role.setId( 2 );
        role.setRole( "ROLE_USER" );
        assertThat( persistedUser.getRoles() ).containsExactly( role );
        assertThat( bCryptPasswordEncoder.matches( oldPassword, persistedUser.getPassword() ) ).isTrue();
    }

    @Test
    public void update_whenCorrectUser_thenSucceed() {
        User user = createUser( 1 );
        becomeUser( user );
        user.getProfile().setName( "batman" );

        User updatedUser = userService.update( user );
        assertThat( updatedUser ).isNotNull();
        assertThat( updatedUser.getProfile().getName() ).isEqualTo( "batman" );
    }

    @Test
    public void changePassword_whenCorrectOldPassword_thenSucceed() {
        User user = createUser( 1 );
        becomeUser( user );

        User updatedUser = userService.changePassword( "imbatman", "newPassword" );
        assertThat( updatedUser ).isNotNull();
        assertThat( bCryptPasswordEncoder.matches( "newPassword", updatedUser.getPassword() ) ).isTrue();
    }

    @Test
    public void changePassword_whenIncorrectOldPassword_thenThrowBadCredentialsException() {
        User user = createUser( 1 );
        becomeUser( user );

        try {
            userService.changePassword( "imsuperman", "newPassword" );
        } catch ( BadCredentialsException e ) {
            // Expected
            return;
        }
        fail( "Should have thrown BadCredentialsException" );
    }

    @Test
    public void changePassword_whenInvalidNewPassword_thenThrowValidationException() {
        User user = createUser( 1 );
        becomeUser( user );

        try {
            userService.changePassword( "imbatman", "12345" );
        } catch ( ValidationException e ) {
            // Expected
            return;
        }
        fail( "Should have thrown ValidationException" );
    }

    @Test
    public void changePasswordByResetToken_whenValidToken_thenSucceed() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        User updatedUser = userService.changePasswordByResetToken( user.getId(), "token1", new PasswordReset( "newPassword", "newPassword" ) );
        assertThat( updatedUser ).isNotNull();
        assertThat( bCryptPasswordEncoder.matches( "newPassword", updatedUser.getPassword() ) ).isTrue();
        verify( passwordResetTokenRepository ).delete( any( PasswordResetToken.class ) );
    }

    @Test(expected = TokenException.class)
    public void changePasswordByResetToken_whenInvalidToken_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.changePasswordByResetToken( user.getId(), "tokenBad", new PasswordReset( "newPassword", "newPassword" ) );
    }

    @Test(expected = TokenException.class)
    public void changePasswordByResetToken_whenInvalidUserId_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.changePasswordByResetToken( user.getId(), "token2", new PasswordReset( "newPassword", "newPassword" ) );
    }

    @Test(expected = TokenException.class)
    public void changePasswordByResetToken_whenExpiredToken_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.changePasswordByResetToken( user.getId(), "token1Expired", new PasswordReset( "newPassword", "newPassword" ) );
    }

    @Test
    public void getCurrentUserName_returnEmail() {
        User user = createUser( 1 );
        becomeUser( user );
        assertThat( userService.getCurrentUserName() ).isEqualTo( user.getEmail() );
    }

    @Test
    public void findCurrentUser_returnCurrentUser() {
        User user = createUser( 1 );
        when( userRepository.findOneWithRoles( 1 ) ).thenReturn( Optional.of( user ) );
        becomeUser( user );
        assertThat( userService.findCurrentUser() ).isEqualTo( user );
        verify( userRepository ).findOneWithRoles( 1 );
    }

    @Test
    public void findUserById_whenValidId_thenReturnUser() {
        User user = createUser( 1 );
        when( userRepository.findById( 1 ) ).thenReturn( Optional.of( user ) );
        assertThat( userService.findUserById( user.getId() ) ).isEqualTo( user );
        verify( userRepository ).findById( 1 );
    }

    @Test
    public void findUserById_whenInvalidId_thenReturnNull() {
        assertThat( userService.findUserById( -1 ) ).isNull();
    }

    @Test
    public void findUserByIdNoAuth_whenValidId_thenReturnUser() {
        User user = createUser( 1 );
        when( userRepository.findOneWithRoles( 1 ) ).thenReturn( Optional.of( user ) );
        assertThat( userService.findUserByIdNoAuth( user.getId() ) ).isEqualTo( user );
        verify( userRepository ).findOneWithRoles( 1 );
    }

    @Test
    public void findUserByIdNoAuth_whenInvalidId_thenReturnNull() {
        assertThat( userService.findUserByIdNoAuth( -1 ) ).isNull();
    }

    @Test
    public void findUserByEmail_whenValidEmail_thenUserShouldBeFound() {
        User user = createUser( 1 );
        when( userRepository.findByEmailIgnoreCase( user.getEmail() ) ).thenReturn( user );

        User found = userService.findUserByEmailNoAuth( user.getEmail() );
        assertThat( found.getEmail() )
                .isEqualTo( user.getEmail() );
    }

    @Test
    public void findUserByEmail_whenInvalidEmail_thenNoUserShouldBeFound() {
        String email = "batman@batcave.org";
        User found = userService.findUserByEmailNoAuth( email );
        assertThat( found ).isNull();
    }

    @Test
    public void findAllNoAuth_thenReturnAllUsers() {
        User user = createUser( 1 );
        when( userRepository.findAll( any( Pageable.class ) ) )
                .thenAnswer( a -> new PageImpl<>( Collections.singletonList( user ), a.getArgument( 0, Pageable.class ), 1 ) );
        assertThat( userService.findAllNoAuth( PageRequest.ofSize( 20 ) ) ).containsExactly( user );
    }

    @Test
    public void convertTerms_whenTermsEmpty_thenReturnEmptySet() {
        //noinspection unchecked
        assertThat( userService.convertTerms( createUser( 1 ), createTaxon( 1 ), Collections.EMPTY_SET ) ).isEmpty();
    }

    @Test
    public void convertTerms_whenTerms_thenReturnCorrectUserTerms() {
        Collection<GeneOntologyTermInfo> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTerm( toGOId( nbr ) )
        ).collect( Collectors.toSet() );
        Collection<UserTerm> userTerms = userService.convertTerms( createUser( 1 ), createTaxon( 1 ), terms );

        assertThat( userTerms ).hasSize( terms.size() );
        assertThat( userTerms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) )
                .isEqualTo( terms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) );

    }

    @Test
    @Ignore
    public void convertTerms_whenTermsAndSizeLimit_thenReturnCorrectUserTerms() {
        when( applicationSettings.getGoTermSizeLimit() ).thenReturn( 3L );

        Taxon taxon = createTaxon( 1 );
        Collection<GeneOntologyTermInfo> terms = LongStream.range( 1, 10 ).boxed().map(
                nbr -> {
                    GeneOntologyTermInfo term = createTerm( toGOId( nbr.intValue() ) );
                    when( goService.getSizeInTaxon( term, taxon ) ).thenReturn( nbr );
                    return term;
                }
        ).collect( Collectors.toSet() );

        Collection<UserTerm> userTerms = userService.convertTerms( createUser( 1 ), taxon, terms );

        assertThat( userTerms ).hasSize( 3 );
        assertThat( userTerms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) )
                .containsExactlyInAnyOrder( "GO:0000001", "GO:0000002", "GO:0000003" );

    }

    @Test
    public void convertTerms_whenTermsAndTaxon_thenReturnUserTermsWithTaxon() {
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> userTerms = userService.convertTerms( createUser( 1 ), taxon, Collections.singleton( createTerm( "GO:0000001" ) ) );

        assertThat( userTerms ).hasSize( 1 );
        assertThat( userTerms.iterator().next().getTaxon() ).isEqualTo( taxon );
    }

    @Test
    public void convertTerms_whenTermsAndEmptyGenes_thenReturnUserTermsWithZeroFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );
        Collection<UserTerm> userTerms = userService.convertTerms( user, taxon, Collections.singleton( createTerm( "GO:0000001" ) ) );

        assertThat( userTerms ).hasSize( 1 );
        assertThat( userService.computeTermFrequencyInTaxon( user, userTerms.iterator().next(), taxon ) ).isZero();
    }

    @Test
    public void convertTerms_whenTermsAndGenes_thenReturnUserTermsWithFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );

        GeneOntologyTermInfo term = createTerm( "GO:0000001" );
        GeneInfo gene = createGene( 1, taxon );
        UserGene userGene = createUnpersistedUserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE );

        when( goService.getTerm( term.getGoId() ) ).thenReturn( term );
        when( goService.getTermsForGene( userGene, true ) ).thenReturn( Sets.newSet( term ) );
        when( goService.getSizeInTaxon( term, taxon ) ).thenReturn( 2L );
        when( goService.getGenes( term ) ).thenReturn( Collections.singleton( gene.getGeneId() ) );
        when( goService.getDirectGenes( term ) ).thenReturn( Collections.singleton( gene.getGeneId() ) );
        when( goService.getGenesInTaxon( term, taxon ) ).thenReturn( Collections.singleton( gene.getGeneId() ) );

        user.getUserGenes().put( gene.getGeneId(), userGene );

        Collection<UserTerm> userTerms = userService.convertTerms( user, taxon, Collections.singleton( term ) );

        assertThat( userTerms ).hasSize( 1 );
        assertThat( userTerms.iterator().next() )
                .hasFieldOrPropertyWithValue( "frequency", 1L )
                .hasFieldOrPropertyWithValue( "size", 2L );
    }

    @Test
    public void convertTerms_whenTermAndTaxon_thenReturnUserTermWithTaxon() {
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> ut = userService.convertTerms( createUser( 1 ), taxon, Sets.newSet( createTerm( "GO:0000001" ) ) );

        assertThat( ut ).isNotEmpty();
        assertThat( ut ).first().hasFieldOrPropertyWithValue( "taxon", taxon );
    }

    @Test
    public void convertTerms_whenTermAndEmptyGenes_thenReturnUserTermWithZeroFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );
        Collection<UserTerm> ut = userService.convertTerms( user, taxon, Sets.newSet( createTerm( "GO:0000001" ) ) );

        assertThat( ut ).isNotEmpty();
        ut.forEach( uts -> assertThat( userService.computeTermFrequencyInTaxon( user, uts, taxon ) ).isZero() );
    }

    @Test
    @Ignore
    public void convertTerms_whenTermAndGenes_thenReturnUserTermWithFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );

        GeneInfo gene = createGene( 1, taxon );
        GeneOntologyTermInfo term = createTermWithGenes( "GO:0000001", gene );

        when( goService.getTermsForGene( gene ) ).thenReturn( Sets.newSet( term ) );
        when( goService.getDirectGenes( term ) ).thenReturn( Collections.singleton( gene.getGeneId() ) );

        user.getUserGenes().put( gene.getGeneId(), UserGene.createUserGeneFromGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        Collection<UserTerm> ut = userService.convertTerms( user, taxon, Sets.newSet( term ) );

        assertThat( ut ).isNotEmpty().first();
        assertThat( userService.computeTermFrequencyInTaxon( user, ut.iterator().next(), taxon ) ).isEqualTo( 1 );
    }


    @Test
    public void updateUserProfileAndPublications_whenPublications_thenReplaceAll() {
        User user = createUser( 1 );
        becomeUser( user );
        Publication oldPub = new Publication();
        oldPub.setId( -1 );
        user.getProfile().getPublications().add( oldPub );

        Set<Publication> newPublications = IntStream.range( 1, 10 ).boxed().map(
                nbr -> {
                    Publication pub = new Publication();
                    pub.setId( nbr );
                    return pub;
                }
        ).collect( Collectors.toSet() );

        User updatedUser = userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, user.getProfile(), newPublications, null, null, Locale.getDefault() );

        assertThat( updatedUser.getProfile().getPublications() ).containsExactlyElementsOf( newPublications );

    }

    @Test
    public void updateUserProfileAndPublication_whenOrgansIsEnabled_thenSaveOrgans() {
        when( applicationSettings.getOrgans().isEnabled() ).thenReturn( true );
        User user = createUser( 1 );
        //noinspection unchecked
        when( organInfoService.findByUberonIdIn( anyCollection() ) )
                .thenAnswer( a -> ( (Collection<String>) a.getArgument( 0, Collection.class ) ).stream()
                        .map( id -> createOrgan( id, null, null ) )
                        .collect( Collectors.toSet() ) );
        Set<String> organUberonIds = Sets.newSet( "UBERON:00001", "UBERON:00002" );
        user = userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, user.getProfile(), null, organUberonIds, null, Locale.getDefault() );
        verify( organInfoService ).findByUberonIdIn( organUberonIds );
        assertThat( user.getUserOrgans().values() )
                .extracting( "uberonId" )
                .containsExactlyInAnyOrder( "UBERON:00001", "UBERON:00002" );
    }

    @Test
    public void updateUserProfileAndPublication_whenOrgansIsNotEnabled_thenIgnoreOrgans() {
        when( applicationSettings.getOrgans().isEnabled() ).thenReturn( false );
        User user = createUser( 1 );
        Set<String> organUberonIds = Sets.newSet( "UBERON:00001", "UBERON:00002" );
        user = userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, user.getProfile(), null, organUberonIds, null, Locale.getDefault() );
        verifyNoInteractions( organInfoService );
        assertThat( user.getUserOrgans() ).isEmpty();
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenPrivacyLevelChangeAndCustomizableLevelIsNotEnabled_thenLeavePrivacyLevelAsIs() {
        when( privacySettings.isCustomizableLevel() ).thenReturn( false );
        User user = createUser( 1 );
        assertThat( user.getProfile() ).hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.PUBLIC );

        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.SHARED );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );

        assertThat( user.getProfile() ).hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.PUBLIC );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenPrivacyLevelChange_thenResetGenePrivacyLevels() {
        when( privacySettings.isCustomizableLevel() ).thenReturn( true );
        when( privacySettings.isCustomizableGeneLevel() ).thenReturn( true );
        Taxon taxon = createTaxon( 1 );
        User user = createUserWithGenes( 1, createGene( 1, taxon ), createGene( 2, taxon ) );
        assertThat( user.getUserGenes().get( 1 ) )
                .hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.PRIVATE );

        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.SHARED );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );

        assertThat( user.getProfile() )
                .hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.SHARED );
        assertThat( user.getUserGenes().get( 1 ) )
                .hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.SHARED );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenContactEmailIsSet_thenSendVerificationEmail() {
        User user = createUser( 1 );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setContactEmail( "foo@example.com" );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        verify( userListener ).onContactEmailUpdate( any( OnContactEmailUpdateEvent.class ) );
        assertThat( user.getProfile().isContactEmailVerified() ).isFalse();

        // make sure that if user update its profile later on, he doesn't get spammed
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        verifyNoMoreInteractions( userListener );
        assertThat( user.getProfile().isContactEmailVerified() ).isFalse();
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenContactEmailIsEqualToUserEmail_thenAssumeContactEmailIsVerified() {
        User user = createUser( 1 );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setContactEmail( user.getEmail() );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        assertThat( user.getProfile().isContactEmailVerified() ).isTrue();
        verifyNoInteractions( userListener );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenContactEmailIsNull_thenUnsetContactEmail() {
        User user = createUser( 1 );
        user.getProfile().setContactEmail( "foo@example.com" );
        user.getProfile().setContactEmailVerified( true );
        user.getProfile().setContactEmailVerifiedAt( Instant.now() );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setContactEmail( null );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        assertThat( user.getProfile().getContactEmail() ).isNull();
        assertThat( user.getProfile().isContactEmailVerified() ).isFalse();
        assertThat( user.getProfile().getContactEmailVerifiedAt() ).isNull();
        verifyNoInteractions( userListener );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenContactEmailIsEmpty_thenUnsetContactEmail() {
        User user = createUser( 1 );
        user.getProfile().setContactEmail( "foo@example.com" );
        user.getProfile().setContactEmailVerified( true );
        user.getProfile().setContactEmailVerifiedAt( Instant.now() );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setContactEmail( "" );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        assertThat( user.getProfile().getContactEmail() ).isEmpty();
        assertThat( user.getProfile().isContactEmailVerified() ).isFalse();
        assertThat( user.getProfile().getContactEmailVerifiedAt() ).isNull();
        verifyNoInteractions( userListener );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenResearcherPositionIsSet_thenUpdateResearcherPosition() {
        when( profileSettings.getEnabledResearcherPositions() ).thenReturn( EnumSet.allOf( ResearcherPosition.class ) );
        User user = createUser( 1 );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.setResearcherPosition( ResearcherPosition.PRINCIPAL_INVESTIGATOR );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        assertThat( user.getProfile().getResearcherPosition() ).isEqualTo( ResearcherPosition.PRINCIPAL_INVESTIGATOR );
        verify( profileSettings ).getEnabledResearcherPositions();
        verify( userRepository ).save( user );
    }

    @Test
    public void updateUserProfileAndPublicationsAndOrgans_whenResearcherCategoriesAreSet_thenUpdateResearcherCategories() {
        when( profileSettings.getEnabledResearcherCategories() ).thenReturn( EnumSet.allOf( ResearcherCategory.class ) );
        User user = createUser( 1 );
        Profile profile = new Profile();
        profile.setPrivacyLevel( PrivacyLevelType.PUBLIC );
        profile.getResearcherCategories().add( ResearcherCategory.IN_SILICO );
        userService.updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( user, profile, null, null, null, Locale.getDefault() );
        assertThat( user.getProfile().getResearcherCategories() ).containsExactly( ResearcherCategory.IN_SILICO );
        verify( profileSettings ).getEnabledResearcherCategories();
        verify( userRepository ).save( user );
    }

    @Test
    public void createPasswordResetTokenForUser_hasCorrectExpiration() {
        User user = createUser( 1 );
        PasswordResetToken passwordResetToken = userService.createPasswordResetTokenForUser( user, Locale.getDefault() );

        Instant lowerBound = Instant.now().plus( 2, ChronoUnit.HOURS ).minus( 1, ChronoUnit.MINUTES );
        Instant upperBound = Instant.now().plus( 2, ChronoUnit.HOURS ).plus( 1, ChronoUnit.MINUTES );

        // one minute tolerance
        assertThat( passwordResetToken.getExpiryDate() ).isBetween( lowerBound, upperBound );
        verify( userListener ).onUserPasswordReset( new OnUserPasswordResetEvent( user, passwordResetToken, Locale.getDefault() ) );
    }

    @Test
    public void verifyPasswordResetToken_whenValidToken_thenSucceed() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        userService.verifyPasswordResetToken( user.getId(), "token1" );
    }

    @Test(expected = TokenException.class)
    public void verifyPasswordResetToken_whenInvalidToken_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.verifyPasswordResetToken( user.getId(), "tokenBad" );
    }

    @Test(expected = TokenException.class)
    public void verifyPasswordResetToken_whenInvalidUserId_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.verifyPasswordResetToken( user.getId(), "token2" );
    }

    @Test(expected = TokenException.class)
    public void verifyPasswordResetToken_whenExpiredToken_thenThrowTokenException() throws TokenException {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );
        userService.verifyPasswordResetToken( user.getId(), "token1Expired" );
    }

    @Test
    public void createVerificationTokenForUser_hasCorrectExpiration() {
        User user = createUser( 1 );
        VerificationToken verificationToken = userService.createVerificationTokenForUser( user, Locale.getDefault() );

        Instant lowerBound = Instant.now().plus( 24, ChronoUnit.HOURS ).minus( 1, ChronoUnit.MINUTES );
        Instant upperBound = Instant.now().plus( 24, ChronoUnit.HOURS ).plus( 1, ChronoUnit.MINUTES );

        // one minute tolerance
        assertThat( verificationToken.getExpiryDate() ).isBetween( lowerBound, upperBound );
        verify( userListener ).onRegistrationComplete( any( OnRegistrationCompleteEvent.class ) );
    }

    @Test
    public void confirmVerificationToken_whenValidToken_thenSucceed() throws TokenException {
        setUpVerificationTokenMocks();

        User user = userService.confirmVerificationToken( "token1" );
        assertThat( user.isEnabled() ).isTrue();
        verify( tokenRepository ).delete( any( VerificationToken.class ) );

    }

    @Test(expected = TokenException.class)
    public void confirmVerificationToken_whenInvalidToken_thenThrowTokenException() throws TokenException {
        setUpVerificationTokenMocks();
        userService.confirmVerificationToken( "tokenBad" );
    }

    @Test(expected = TokenException.class)
    public void confirmVerificationToken_whenExpiredToken_thenThrowTokenException() throws TokenException {
        setUpVerificationTokenMocks();
        userService.confirmVerificationToken( "token1Expired" );
    }

    @Test(expected = TokenException.class)
    public void confirmVerificationToken_whenTokenEmailDoesNotMatch_thenThrowTokenException() throws TokenException {
        User user = createUser( 1 );
        VerificationToken token = new VerificationToken();
        token.setUser( user );
        token.setEmail( "foo@example.com" );
        token.updateToken( "token1" );
        when( tokenRepository.findByToken( token.getToken() ) ).thenReturn( token );
        userService.confirmVerificationToken( "token1" );
    }

    @Test
    public void updateTermsAndGenesInTaxon_whenUserHasNoGeneOrTerms() {
        User user = createUser( 1 );
        becomeUser( user );
        Taxon taxon = createTaxon( 1 );

        // Mock goService.getRelatedGenes
        Collection<GeneInfo> calculatedGenes = IntStream.range( 101, 110 ).boxed().map(
                nbr -> createGene( nbr, taxon )
        ).collect( Collectors.toList() );
        when( goService.getGenesInTaxon( Mockito.anyCollection(), any() ) ).thenReturn( calculatedGenes.stream().map( GeneInfo::getGeneId ).collect( Collectors.toList() ) );

        Collection<GeneOntologyTermInfo> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTermWithGenes( toGOId( nbr ), createGene( 100 + nbr, taxon ) )
        ).collect( Collectors.toSet() );
        when( geneInfoService.load( any( Integer.class ) ) ).thenAnswer( a -> createGene( a.getArgument( 0, Integer.class ), taxon ) );

        for ( GeneOntologyTermInfo term : terms ) {
            when( goService.getTerm( term.getGoId() ) ).thenReturn( term );
        }

        Map<GeneInfo, TierType> geneTierMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<GeneInfo, PrivacyLevelType> privacyLevelMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> PrivacyLevelType.PRIVATE ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<GeneInfo, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

    }

    @Test
    @Ignore
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTerms() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        GeneInfo geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGenes( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, user, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        GeneInfo geneWillChangeTier = createGene( 5, taxon );
        GeneOntologyTerm termWillRemain = createTermWithGenes( toGOId( geneWillChangeTier.getGeneId() ), geneWillChangeTier );
        user.getUserTerms().add( createUserTerm( 2, user, termWillRemain, taxon ) );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<GeneInfo> calculatedGenes = IntStream.range( 101, 110 ).boxed().map(
                nbr -> createGene( nbr, taxon )
        ).collect( Collectors.toList() );
        for ( GeneInfo gi : calculatedGenes ) {
            when( geneInfoService.load( gi.getGeneId() ) ).thenReturn( gi );
        }
        when( goService.getGenesInTaxon( anyCollection(), any() ) ).thenReturn( calculatedGenes.stream().map( GeneInfo::getGeneId ).collect( Collectors.toList() ) );

        Collection<GeneOntologyTermInfo> terms = IntStream.range( 1, 10 ).boxed().map( nbr -> {
            GeneInfo g = createGene( nbr, taxon );
            GeneOntologyTermInfo gt = createTermWithGenes( toGOId( nbr ), g );
            when( geneInfoService.load( g.getGeneId() ) ).thenReturn( g );
            return gt;
        } ).collect( Collectors.toSet() );

        Map<GeneInfo, TierType> geneTierMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<GeneInfo, PrivacyLevelType> privacyLevelMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> PrivacyLevelType.PUBLIC ) );

        assertThat( geneTierMap ).hasSize( 10 );
        assertThat( geneTierMap ).hasSameSizeAs( privacyLevelMap );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<GeneInfo, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

    }

    @Test
    @Ignore
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTermsInMultipleTaxon() {
        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        GeneInfo geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGenes( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, user, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );
        when( geneInfoService.load( 999 ) ).thenReturn( geneWillBeRemoved );

        GeneInfo geneWillChangeTier = createGene( 5, taxon );
        GeneOntologyTerm termWillRemain = createTermWithGenes( toGOId( geneWillChangeTier.getGeneId() ), geneWillChangeTier );
        user.getUserTerms().add( createUserTerm( 2, user, termWillRemain, taxon ) );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );
        when( geneInfoService.load( 5 ) ).thenReturn( geneWillChangeTier );

        // Taxon 2
        Taxon taxon2 = createTaxon( 2 );
        GeneInfo geneOtherTaxon = createGene( 9999, taxon2 );
        GeneOntologyTerm termOtherTaxon = createTermWithGenes( toGOId( geneOtherTaxon.getGeneId() ), geneOtherTaxon );
        user.getUserTerms().add( createUserTerm( 3, user, termOtherTaxon, taxon2 ) );
        user.getUserGenes().put( geneOtherTaxon.getGeneId(), createUserGene( 3, geneOtherTaxon, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );
        when( geneInfoService.load( 9999 ) ).thenReturn( geneOtherTaxon );

        GeneInfo geneOtherTaxon2 = createGene( 5555, taxon2 );
        GeneOntologyTerm termOtherTaxon2 = createTermWithGenes( toGOId( geneOtherTaxon2.getGeneId() ), geneOtherTaxon2 );
        user.getUserTerms().add( createUserTerm( 4, user, termOtherTaxon2, taxon2 ) );
        user.getUserGenes().put( geneOtherTaxon2.getGeneId(), createUserGene( 4, geneOtherTaxon2, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );
        when( geneInfoService.load( 5555 ) ).thenReturn( geneOtherTaxon2 );

        becomeUser( user );

        Collection<GeneOntologyTermInfo> terms = IntStream.range( 1, 10 )
                .boxed()
                .map( nbr -> createTermWithGenes( toGOId( nbr ), createGene( nbr, taxon ) ) )
                .collect( Collectors.toSet() );

        when( goService.getGenesInTaxon( anyCollection(), any() ) )
                .then( i -> {
                    @SuppressWarnings("unchecked") Collection<GeneOntologyTermInfo> whenTerms = i.getArgument( 0, Collection.class );
                    return whenTerms.stream().map( goService::getDirectGenes ).collect( Collectors.toSet() );
                } );

        Map<GeneInfo, TierType> geneTierMap = Maps.newHashMap( geneWillChangeTier, TierType.TIER1 );

        Map<GeneInfo, PrivacyLevelType> privacyLevelMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> PrivacyLevelType.PRIVATE ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );
        assertThatUserTermsAreEqualTo( user, taxon2, Sets.newSet( termOtherTaxon, termOtherTaxon2 ) );

        Map<GeneInfo, TierType> expectedGenes = new HashMap<>( geneTierMap );
        terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .forEach( g -> expectedGenes.putIfAbsent( createGene( g, taxon ), TierType.TIER3 ) );

        Map<GeneInfo, TierType> expectedGenesTaxon2 = new HashMap<>();
        expectedGenesTaxon2.put( geneOtherTaxon, TierType.TIER1 );
        expectedGenesTaxon2.put( geneOtherTaxon2, TierType.TIER3 );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );
        assertThatUserGenesAreEqualTo( user, taxon2, expectedGenesTaxon2 );

    }

    @Test
    @Ignore
    public void updateTermsAndGenesInTaxon_whenOldAndNewOverlap_thenRetainIds() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        // These should retain ids so that Hibernate does not delete and re insert rows
        GeneInfo geneOverlapsManual = createGene( 5, taxon );
        GeneOntologyTerm termOverlapsManual = createTermWithGenes( toGOId( geneOverlapsManual.getGeneId() ), geneOverlapsManual );
        user.getUserTerms().add( createUserTerm( 1, user, termOverlapsManual, taxon ) );
        user.getUserGenes().put( geneOverlapsManual.getGeneId(), createUserGene( 1, geneOverlapsManual, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        GeneInfo geneOverlapsCalculated = createGene( 105, taxon );
        GeneOntologyTerm termOverlapsCalculated = createTermWithGenes( toGOId( geneOverlapsCalculated.getGeneId() ), geneOverlapsCalculated );
        user.getUserTerms().add( createUserTerm( 2, user, termOverlapsCalculated, taxon ) );
        user.getUserGenes().put( geneOverlapsCalculated.getGeneId(), createUserGene( 2, geneOverlapsCalculated, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<GeneInfo> calculatedGenes = Collections.singleton( createGene( 105, taxon ) );
        when( goService.getGenesInTaxon( anyCollection(), any() ) ).thenReturn( calculatedGenes.stream().map( GeneInfo::getGeneId ).collect( Collectors.toList() ) );

        Collection<GeneOntologyTermInfo> terms = Collections.singleton( createTermWithGenes( toGOId( 5 ), createGene( 5, taxon ) ) );

        Map<GeneInfo, TierType> geneTierMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<GeneInfo, PrivacyLevelType> privacyLevelMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> PrivacyLevelType.PRIVATE ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        // Might as well test this
        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<GeneInfo, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

        // This is really why we're here
        assertThat( user.getUserTerms().iterator().next().getId() ).isEqualTo( 1 );
        assertThat( user.getUserGenes().values().iterator().next().getId() ).isEqualTo( 1 );
    }

    @Test
    @Ignore
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTerms_thenUpdateFrequency() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        GeneInfo geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGenes( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, user, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        GeneInfo geneWillChangeTier = createGene( 5, taxon );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER2, PrivacyLevelType.PRIVATE ) );

        GeneOntologyTermInfo termInfoWillUpdateFrequency = createTermWithGenes( toGOId( geneWillChangeTier.getGeneId() ), geneWillBeRemoved, geneWillChangeTier );
        UserTerm termWillUpdateFrequency = createUserTerm( 3, user, termInfoWillUpdateFrequency, taxon );
        // Should have frequency of 2
        user.getUserTerms().add( termWillUpdateFrequency );

        becomeUser( user );

        when( goService.getGenesInTaxon( anyCollection(), any() ) ).thenReturn( Collections.emptySet() );

        Map<GeneInfo, TierType> geneTierMap = Maps.newHashMap( geneWillChangeTier, TierType.TIER1 );
        Map<GeneInfo, PrivacyLevelType> genePrivacyLevelTypeMap = Maps.newHashMap( geneWillChangeTier, PrivacyLevelType.PRIVATE );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, genePrivacyLevelTypeMap, Sets.newSet( termInfoWillUpdateFrequency ) );

        assertThat( user.getUserTerms() ).hasSize( 1 );
        assertThat( user.getUserTerms().iterator().next() ).isEqualTo( termWillUpdateFrequency );
        assertThat( userService.computeTermFrequencyInTaxon( user, user.getUserTerms().iterator().next(), taxon ) ).isEqualTo( 1 );
    }

    @Test
    @Ignore
    public void updateTermsAndGenesInTaxon_whenTermsOverlapInDifferentSpecies_thenKeepBothTerms() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Taxon taxon2 = createTaxon( 2 );

        // These should retain ids so that Hibernate does not delete and re insert rows
        GeneInfo geneInTaxon1 = createGene( 5, taxon );
        when( geneInfoService.load( 5 ) ).thenReturn( geneInTaxon1 );
        GeneOntologyTerm termInTaxon1 = createTermWithGenes( toGOId( geneInTaxon1.getGeneId() ), geneInTaxon1 );
        user.getUserTerms().add( createUserTerm( 1, user, termInTaxon1, taxon ) );
        user.getUserGenes().put( geneInTaxon1.getGeneId(), createUserGene( 1, geneInTaxon1, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        GeneInfo geneInTaxon2 = createGene( 105, taxon2 );
        when( geneInfoService.load( 105 ) ).thenReturn( geneInTaxon2 );
        GeneOntologyTerm termInTaxon2 = createTermWithGenes( toGOId( geneInTaxon2.getGeneId() ), geneInTaxon2 );
        user.getUserTerms().add( createUserTerm( 2, user, termInTaxon2, taxon2 ) );
        user.getUserGenes().put( geneInTaxon2.getGeneId(), createUserGene( 2, geneInTaxon2, user, TierType.TIER3, PrivacyLevelType.PRIVATE ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        GeneInfo gene205InTaxon1 = createGene( 205, taxon );
        Collection<GeneInfo> calculatedGenes = Collections.singleton( gene205InTaxon1 );
        when( geneInfoService.load( 205 ) ).thenReturn( gene205InTaxon1 );
        when( goService.getGenesInTaxon( anyCollection(), eq( taxon ) ) ).thenReturn( calculatedGenes.stream().map( GeneInfo::getGeneId ).collect( Collectors.toList() ) );
        when( goService.getGenesInTaxon( anyCollection(), eq( taxon2 ) ) ).thenReturn( Collections.emptySet() );

        // Attempting to add term to taxon 1 that is already present in taxon 2
        GeneInfo g1 = createGene( 1005, taxon );
        when( geneInfoService.load( g1.getGeneId() ) ).thenReturn( g1 );
        Collection<GeneOntologyTermInfo> terms = Collections.singleton( createTermWithGenes( toGOId( 105 ), g1 ) );

        Map<GeneInfo, TierType> geneTierMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER3 ) );

        Map<GeneInfo, PrivacyLevelType> privacyLevelMap = terms.stream()
                .flatMap( t -> goService.getDirectGenes( t ).stream() )
                .map( geneId -> createGene( geneId, taxon ) )
                .collect( Collectors.toMap( Function.identity(), g -> PrivacyLevelType.PRIVATE ) );

        user = userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        // Why we are here
        assertThatUserTermsAreEqualTo( user, taxon, terms );
        assertThatUserTermsAreEqualTo( user, taxon2, Collections.singleton( termInTaxon2 ) );

        Map<GeneInfo, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );
        assertThatUserGenesAreEqualTo( user, taxon2, Maps.newHashMap( geneInTaxon2, TierType.TIER3 ) );

    }

    @Test
    public void updateTermsAndGenesInTaxon_whenGeneIsAlsoDefinedInTerms_thenKeepGeneTierLevel() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        GeneInfo gene = createGene( 1, taxon );
        GeneOntologyTermInfo term = createTermWithGenes( "GO:0000001", gene );
        when( goService.getTerm( term.getGoId() ) ).thenReturn( term );
        when( goService.getGenes( term ) ).thenReturn( Collections.singletonList( gene.getGeneId() ) );
        UserTerm userTerm = createUserTerm( 1, user, term, taxon );
        UserGene userGene = createUserGene( 1, gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        assertThat( user.getUserGenes() ).isEmpty();
        userService.updateTermsAndGenesInTaxon( user, taxon, Maps.newHashMap( gene, TierType.TIER1 ),
                Maps.newHashMap( gene, PrivacyLevelType.PRIVATE ), Sets.newSet( term ) );
        assertThat( user.getUserGenes().get( 1 ) )
                .isEqualTo( userGene )
                .hasFieldOrPropertyWithValue( "tier", TierType.TIER1 )
                .hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.PRIVATE );
        assertThat( user.getUserTerms() )
                .containsExactly( userTerm );
        verify( userRepository ).save( user );
    }

    private void assertThatUserTermsAreEqualTo( User user, Taxon taxon, Collection<? extends GeneOntologyTerm> terms ) {
        Collection<UserTerm> expectedUserTerms = terms.stream()
                .map( t -> createUnpersistedUserTerm( user, t, taxon ) )
                .collect( Collectors.toSet() );
        assertThat( user.getTermsByTaxon( taxon ) )
                .usingElementComparatorIgnoringFields( "id", "frequency", "size" )
                .containsAll( expectedUserTerms )
                .containsOnlyElementsOf( expectedUserTerms );
    }

    private void assertThatUserGenesAreEqualTo( User user, Taxon taxon, Map<GeneInfo, TierType> expectedGenes ) {
        Set<UserGene> expectedUserGenes = expectedGenes.entrySet().stream()
                .map( e -> createUnpersistedUserGene( e.getKey(), user, e.getValue(), PrivacyLevelType.PRIVATE ) )
                .collect( Collectors.toSet() );
        assertThat( user.getGenesByTaxon( taxon ) )
                .usingElementComparatorIgnoringFields( "id" )
                .containsAll( expectedUserGenes )
                .containsOnlyElementsOf( expectedUserGenes );
    }

    @Test
    @Ignore
    public void recommendTerms_thenReturnBestResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );
    }

    @Test
    public void recommendTerms_whenMinSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 12, -1, -1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );

        found = userService.recommendTerms( user, taxon, 20, -1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test
    @Ignore
    public void recommendTerms_whenMaxSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, 12, -1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );

        found = userService.recommendTerms( user, taxon, -1, 1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenFrequencyLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, -1, 3 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );

        found = userService.recommendTerms( user, taxon, -1, -1, 4 );
        assertThat( found ).isEmpty();
    }

    @Test
    @Ignore
    public void recommendTerms_whenFrequencyLimitedAndSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 11, 12, 2 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );

        found = userService.recommendTerms( user, taxon, 1, 11, 2 );
        assertThat( found ).isEmpty();
    }

    @Test
    @Ignore
    public void recommendTerms_whenRedundantTerms_thenReturnOnlyMostSpecific() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 11, 11, 1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 2 ), toGOId( 3 ), toGOId( 5 ), toGOId( 99 ) );

        found = userService.recommendTerms( user, taxon, 1, 11, 2 );
        assertThat( found ).isEmpty();
    }

    @Test
    @Ignore
    public void recommendTerms_whenUserHasSomeTopTerms_thenReturnNewBestResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        user.getUserTerms().add( createUserTerm( 1, user, createTerm( toGOId( 1 ) ), taxon ) );

        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 7 ), toGOId( 8 ) );
    }

    @Test
    public void recommendTerms_whenUserHasAllTopTerms_thenReturnNextBestResultsOnly() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        user.getUserTerms().add( createUserTerm( 1, user, createTerm( toGOId( 1 ) ), taxon ) );
        user.getUserTerms().add( createUserTerm( 2, user, createTerm( toGOId( 7 ) ), taxon ) );
        user.getUserTerms().add( createUserTerm( 3, user, createTerm( toGOId( 8 ) ), taxon ) );

        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );
    }

    @Test
    public void recommendTerms_whenUserHasNoGenes_thenReturnEmpty() {
        Map<GeneOntologyTermInfo, Long> empyFMap = new HashMap<>();
        when( goService.termFrequencyMap( anyCollection() ) ).thenReturn( empyFMap );

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, -1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test(expected = NullPointerException.class)
    public void recommendTerms_whenUserNull_thenThrowNullPointerException() {
        setUpRecommendTermsMocks();

        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( null, taxon, -1, -1, -1 );
        assertThat( found ).isNull();
    }

    @Test(expected = NullPointerException.class)
    public void recommendTerms_whenTaxonNull_thenThrowNullPointerException() {
        setUpRecommendTermsMocks();

        User user = createUser( 1 );
        userService.recommendTerms( user, null, -1, -1, -1 );
    }


    @Test
    public void updateUserTerms_thenSucceed() {
        userService.updateUserTerms();
        verify( userRepository ).findAllWithUserTerms();
    }

    @Test
    public void anonymizeUser_thenReturnAnonymizedUser() {
        when( privacySettings.getDefaultLevel() ).thenReturn( 2 );
        when( privacySettings.isEnableAnonymizedSearchResults() ).thenReturn( true );
        User user = createUser( 1 );
        user.setEnabled( true );
        user.setEnabledAt( Instant.now() );
        user.getProfile().setPrivacyLevel( PrivacyLevelType.PRIVATE );
        User anonymizedUser = userService.anonymizeUser( user );
        assertThat( anonymizedUser )
                .hasFieldOrPropertyWithValue( "enabled", true )
                .hasFieldOrPropertyWithValue( "enabledAt", null )
                .hasFieldOrPropertyWithValue( "email", null )
                .hasFieldOrPropertyWithValue( "profile.privacyLevel", PrivacyLevelType.PUBLIC );
        assertThat( anonymizedUser.getUserGenes() ).isEmpty();
        assertThat( userService.findUserByAnonymousIdNoAuth( anonymizedUser.getAnonymousId() ) )
                .isEqualTo( user );
    }

    @Test
    public void anonymizeUserGene_thenReturnAnonymizedUserGene() {
        when( privacySettings.getDefaultLevel() ).thenReturn( 2 );
        when( privacySettings.isEnableAnonymizedSearchResults() ).thenReturn( true );
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );
        UserGene userGene = createUserGene( 1, createGene( 1, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        UserGene anonymizedUserGene = userService.anonymizeUserGene( userGene );
        assertThat( anonymizedUserGene )
                .hasFieldOrPropertyWithValue( "privacyLevel", PrivacyLevelType.PUBLIC );
        assertThat( anonymizedUserGene.getUser() )
                .isEqualToIgnoringGivenFields( userService.anonymizeUser( user ), "anonymousId" );
        assertThat( userService.findUserGeneByAnonymousIdNoAuth( anonymizedUserGene.getAnonymousId() ) )
                .isEqualTo( userGene );
    }

    @Test
    public void sendGeneAccessRequest_thenTriggerOnRequestAccessEvent() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        UserGene userGene = createUserGene( 1, createGene( 1, taxon ), user, TierType.TIER1, PrivacyLevelType.PRIVATE );
        userService.sendGeneAccessRequest( user, userGene, "Because." );
        ArgumentCaptor<OnRequestAccessEvent> captor = ArgumentCaptor.forClass( OnRequestAccessEvent.class );
        verify( userListener ).onGeneRequestAccess( captor.capture() );
        assertThat( captor.getValue() )
                .hasFieldOrPropertyWithValue( "user", user )
                .hasFieldOrPropertyWithValue( "object", userGene )
                .hasFieldOrPropertyWithValue( "reason", "Because." );
    }

    private GeneOntologyTermInfo createTermWithGenes( String id, GeneInfo... genes ) {
        GeneOntologyTermInfo term = createTerm( id );
        when( goService.getDirectGenes( term ) ).thenReturn( Arrays.stream( genes ).map( GeneInfo::getGeneId ).collect( Collectors.toSet() ) );
        when( goService.getSizeInTaxon( eq( term ), any( Taxon.class ) ) ).thenAnswer( a -> Arrays.stream( genes ).filter( g -> g.getTaxon().equals( a.getArgument( 1, Taxon.class ) ) ).count() );
        return term;
    }
}
