package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.PasswordResetTokenRepository;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.repositories.VerificationTokenRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.BaseTest;

import javax.validation.ValidationException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
public class UserServiceImplTest extends BaseTest {

    private static Log log = LogFactory.getLog( UserServiceImplTest.class );

    @TestConfiguration
    static class UserServiceImplTestContextConfiguration {

        @Bean
        public PrivacyService privacyService() { return new PrivacyServiceImpl(); }

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }

        @Bean
        public BCryptPasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

    }

    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private UserService userService;
    @SuppressWarnings("SpringJavaAutowiredMembersInspection")
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private UserRepository userRepository;
    @MockBean
    private RoleRepository roleRepository;
    @MockBean
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @MockBean
    private VerificationTokenRepository tokenRepository;
    @MockBean
    private GOService goService;
    @MockBean
    private ApplicationSettings applicationSettings;

    @Before
    public void setUp() {
        User user = createUser( 1 );

        Mockito.when( userRepository.findOne( user.getId() ) ).thenReturn( user );
        Mockito.when( userRepository.save( Mockito.any( User.class ) ) ).then( i -> i.getArgumentAt( 0, User.class ) );
        Mockito.when( passwordResetTokenRepository.save( Mockito.any( PasswordResetToken.class ) ) ).then( i -> i.getArgumentAt( 0, PasswordResetToken.class ) );
        Mockito.when( tokenRepository.save( Mockito.any( VerificationToken.class ) ) ).then( i -> i.getArgumentAt( 0, VerificationToken.class ) );

        Mockito.when( applicationSettings.getGoTermSizeLimit() ).thenReturn( 100 );
    }

    private void setUpRoleMocks() {
        Role role = new Role();
        role.setId( 2 );
        role.setRole( "ROLE_USER" );
        Mockito.when( roleRepository.findByRole( "ROLE_USER" ) ).thenReturn( role );
    }

    private void setUpPasswordResetTokenMocks() {
        User user = createUser( 1 );
        User otherUser = createUser( 2 );
        PasswordResetToken token = new PasswordResetToken();
        token.setUser( user );
        token.updateToken( "token1" );
        Mockito.when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new PasswordResetToken();
        token.setUser( user );
        token.setToken( "token1Expired" );
        token.setExpiryDate( new Date() );
        Mockito.when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new PasswordResetToken();
        token.setUser( otherUser );
        token.updateToken( "token2" );
        Mockito.when( passwordResetTokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        Mockito.when( passwordResetTokenRepository.findByToken( "tokenBad" ) ).thenReturn( null );
    }

    private void setUpVerificationTokenMocks() {
        User user = createUser( 1 );
        User otherUser = createUser( 2 );
        VerificationToken token = new VerificationToken();
        token.setUser( user );
        token.updateToken( "token1" );
        Mockito.when( tokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        token = new VerificationToken();
        token.setUser( user );
        token.setToken( "token1Expired" );
        token.setExpiryDate( new Date() );
        Mockito.when( tokenRepository.findByToken( token.getToken() ) ).thenReturn( token );

        Mockito.when( tokenRepository.findByToken( "tokenBad" ) ).thenReturn( null );
    }

    private void setUpRecomendTermsMocks() {
        Map<GeneOntologyTerm, Long> expected = new HashMap<>();
        Taxon taxon = createTaxon( 1 );

        expected.put( createTerm( toGOId( 0 ) ), 2L );
        expected.put( createTerm( toGOId( 1 ) ), 3L );
        expected.put( createTerm( toGOId( 2 ) ), 1L );
        expected.put( createTerm( toGOId( 3 ) ), 1L );
        expected.put( createTerm( toGOId( 4 ) ), 2L );
        expected.put( createTerm( toGOId( 5 ) ), 1L );
        expected.put( createTerm( toGOId( 6 ) ), 2L );
        expected.put( createTerm( toGOId( 7 ) ), 3L );
        expected.put( createTerm( toGOId( 8 ) ), 3L );

        // These will make sure we remove redundant terms
        GeneOntologyTerm t98 = createTerm( toGOId( 98 ) );
        GeneOntologyTerm t99 = createTerm( toGOId( 99 ) );
        expected.put( t98, 1L );
        expected.put( t99, 1L );
        Mockito.when( goService.getDescendants( t98 ) ).thenReturn( Collections.singleton( t99 ) );

        expected.forEach( ( key, value ) -> key.getSizesByTaxon().put( taxon, value + 10 ) );

        Mockito.when( goService.termFrequencyMap( Mockito.anyCollectionOf( Gene.class ) ) ).thenReturn( expected );
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
    public void update_whenWrongUser_thenFail() {
        User user = createUser( 1 );
        becomeUser( user );

        user = createUser( 1 );
        user.setEmail( "wrongemail@email.com" );
        user.getProfile().setName( "batman" );

        User updatedUser = userService.update( user );
        assertThat( updatedUser ).isNull();
    }

    @Test
    public void update_whenCorrectUserthenSucceed() {
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
        } catch (BadCredentialsException e) {
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
        } catch (ValidationException e) {
            // Expected
            return;
        }
        fail( "Should have thrown ValidationException" );
    }

    @Test
    public void changePasswordByResetToken_whenValidToken_thenSucceed() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        User updatedUser = userService.changePasswordByResetToken( user.getId(), "token1", "newPassword" );
        assertThat( updatedUser ).isNotNull();
        assertThat( bCryptPasswordEncoder.matches( "newPassword", updatedUser.getPassword() ) ).isTrue();
    }

    @Test
    public void changePasswordByResetToken_whenInvalidToken_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.changePasswordByResetToken( user.getId(), "tokenBad", "newPassword" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void changePasswordByResetToken_whenInvalidUserId_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.changePasswordByResetToken( user.getId(), "token2", "newPassword" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void changePasswordByResetToken_whenExpiredToken_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.changePasswordByResetToken( user.getId(), "token1Expired", "newPassword" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void changePasswordByResetToken_whenInvalidNewPassword_thenThrowValidationException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.changePasswordByResetToken( user.getId(), "token1", "12345" );
        } catch (ValidationException e) {
            // Expected
            return;
        }
        fail( "Should have thrown ValidationException" );
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
        becomeUser( user );
        assertThat( userService.findCurrentUser() ).isEqualTo( user );
    }

    @Test
    public void findUserById_whenValidId_thenReturnUser() {
        User user = createUser( 1 );
        assertThat( userService.findUserById( user.getId() ) ).isEqualTo( user );
    }

    @Test
    public void findUserById_whenInvalidId_thenReturnNull() {
        assertThat( userService.findUserById( -1 ) ).isNull();
    }

    @Test
    public void findUserByIdNoAuth_whenValidId_thenReturnUser() {
        User user = createUser( 1 );
        assertThat( userService.findUserByIdNoAuth( user.getId() ) ).isEqualTo( user );
    }

    @Test
    public void findUserByIdNoAuth_whenInvalidId_thenReturnNull() {
        assertThat( userService.findUserByIdNoAuth( -1 ) ).isNull();
    }

    @Test
    public void findUserByEmail_whenValidEmail_thenUserShouldBeFound() {
        User user = createUser( 1 );
        Mockito.when( userRepository.findByEmailIgnoreCase( user.getEmail() ) ).thenReturn( user );

        User found = userService.findUserByEmail( user.getEmail() );
        assertThat( found.getEmail() )
                .isEqualTo( user.getEmail() );
    }

    @Test
    public void findUserByEmail_whenInvalidEmail_thenNoUserShouldBeFound() {
        String email = "batman@batcave.org";
        User found = userService.findUserByEmail( email );
        assertThat( found ).isNull();
    }

    @Test
    public void findAll_thenReturnAllUsers() {
        User user = createUser( 1 );
        Mockito.when( userRepository.findAll() ).thenReturn( Collections.singletonList( user ) );

        assertThat( userService.findAll() ).containsExactly( user );
    }

    @Test
    public void convertTerms_whenTermsEmpty_thenReturnEmptySet() {
        //noinspection unchecked
        assertThat( userService.convertTerms( createUser( 1 ), createTaxon( 1 ), Collections.EMPTY_SET ) ).isEmpty();
    }

    @Test
    public void convertTerms_whenTerms_thenReturnCorrectUserTerms() {
        Collection<GeneOntologyTerm> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTerm( toGOId( nbr ) )
        ).collect( Collectors.toSet() );
        Collection<UserTerm> userTerms = userService.convertTerms( createUser( 1 ), createTaxon( 1 ), terms );

        assertThat( userTerms ).hasSize( terms.size() );
        assertThat( userTerms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) )
                .isEqualTo( terms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) );

    }

    @Test
    public void convertTerms_whenTermsAndSizeLimit_thenReturnCorrectUserTerms() {
        Mockito.when( applicationSettings.getGoTermSizeLimit() ).thenReturn( 3 );

        Taxon taxon = createTaxon( 1 );
        Collection<GeneOntologyTerm> terms = LongStream.range( 1, 10 ).boxed().map(
                nbr -> {
                    GeneOntologyTerm term = createTerm( toGOId( nbr.intValue() ) );
                    term.getSizesByTaxon().put( taxon, nbr );
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
        Collection<UserTerm> userTerms = userService.convertTerms( createUser( 1 ), taxon, Collections.singleton( createTerm( "GO:0000001" ) ) );

        assertThat( userTerms ).hasSize( 1 );
        assertThat( userTerms.iterator().next().getFrequency() ).isZero();
    }

    @Test
    public void convertTerms_whenTermsAndGenes_thenReturnUserTermsWithFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );

        GeneOntologyTerm term = createTerm( "GO:0000001" );
        Gene gene = createGene( 1, taxon );

        gene.getTerms().add( term );
        term.getDirectGenes().add( gene );

        user.getUserGenes().put( gene.getGeneId(), new UserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        Collection<UserTerm> userTerms = userService.convertTerms( user, taxon, Collections.singleton( term ) );

        assertThat( userTerms ).hasSize( 1 );
        assertThat( userTerms.iterator().next().getFrequency() ).isEqualTo( 1 );

    }

    @Test
    public void convertTerms_whenTermAndTaxon_thenReturnUserTermWithTaxon() {
        Taxon taxon = createTaxon( 1 );
        UserTerm ut = userService.convertTerms( createUser( 1 ), taxon, createTerm( "GO:0000001" ) );

        assertThat( ut ).isNotNull();
        assertThat( ut.getTaxon() ).isEqualTo( taxon );
    }

    @Test
    public void convertTerms_whenTermAndEmptyGenes_thenReturnUserTermWithZeroFrequency() {
        Taxon taxon = createTaxon( 1 );
        UserTerm ut = userService.convertTerms( createUser( 1 ), taxon, createTerm( "GO:0000001" ) );

        assertThat( ut ).isNotNull();
        assertThat( ut.getFrequency() ).isZero();
    }

    @Test
    public void convertTerms_whenTermAndGenes_thenReturnUserTermWithFrequency() {
        Taxon taxon = createTaxon( 1 );
        User user = createUser( 1 );

        GeneOntologyTerm term = createTerm( "GO:0000001" );
        Gene gene = createGene( 1, taxon );

        gene.getTerms().add( term );
        term.getDirectGenes().add( gene );

        user.getUserGenes().put( gene.getGeneId(), new UserGene( gene, user, TierType.TIER1, PrivacyLevelType.PRIVATE ) );

        UserTerm ut = userService.convertTerms( user, taxon, term );

        assertThat( ut ).isNotNull();
        assertThat( ut.getFrequency() ).isEqualTo( 1 );

    }


    @Test
    public void updatePublications_whenPublications_thenReplaceAll() {
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

        User updatedUser = userService.updatePublications( user, newPublications );

        assertThat( updatedUser.getProfile().getPublications() ).containsExactlyElementsOf( newPublications );

    }

    @Test
    public void updatePublications_whenWrongUser_thenFail() {
        User user = createUser( 1 );

        User otherUser = createUser( 2 );
        otherUser.setEmail( "wrongemail@email.ca" );
        becomeUser( otherUser );

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

        User updatedUser = userService.updatePublications( user, newPublications );

        assertThat( updatedUser ).isNull();

    }

    @Test
    public void createPasswordResetTokenForUser_hasCorrectExpiration() {
        User user = createUser( 1 );
        String token = "HEYYEYAAEYAAAEYAEYAA";
        PasswordResetToken passwordResetToken = userService.createPasswordResetTokenForUser( user, token );

        final Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis( new Date().getTime() );
        cal.add( Calendar.HOUR, PasswordResetToken.EXPIRATION );
        Date expectedExpiry = new Date( cal.getTime().getTime() );

        cal.setTimeInMillis( expectedExpiry.getTime() );
        cal.add( Calendar.MINUTE, -1 );
        Date lowerBound = new Date( cal.getTime().getTime() );

        cal.setTimeInMillis( expectedExpiry.getTime() );
        cal.add( Calendar.MINUTE, 1 );
        Date upperBound = new Date( cal.getTime().getTime() );

        // one minute tolerance
        assertThat( passwordResetToken.getExpiryDate() ).isBetween( lowerBound, upperBound );

    }

    @Test
    public void verifyPasswordResetToken_whenValidToken_thenSucceed() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        userService.verifyPasswordResetToken( user.getId(), "token1" );
    }

    @Test
    public void verifyPasswordResetToken_whenInvalidToken_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.verifyPasswordResetToken( user.getId(), "tokenBad" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void verifyPasswordResetToken_whenInvalidUserId_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.verifyPasswordResetToken( user.getId(), "token2" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void verifyPasswordResetToken_whenExpiredToken_thenThrowTokenException() {
        setUpPasswordResetTokenMocks();
        User user = createUser( 1 );

        try {
            userService.verifyPasswordResetToken( user.getId(), "token1Expired" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void createVerificationTokenForUser_hasCorrectExpiration() {
        User user = createUser( 1 );
        String token = "HEYYEYAAEYAAAEYAEYAA";
        VerificationToken verificationToken = userService.createVerificationTokenForUser( user, token );

        final Calendar cal = Calendar.getInstance();

        cal.setTimeInMillis( new Date().getTime() );
        cal.add( Calendar.HOUR, VerificationToken.EXPIRATION );
        Date expectedExpiry = new Date( cal.getTime().getTime() );

        cal.setTimeInMillis( expectedExpiry.getTime() );
        cal.add( Calendar.MINUTE, -1 );
        Date lowerBound = new Date( cal.getTime().getTime() );

        cal.setTimeInMillis( expectedExpiry.getTime() );
        cal.add( Calendar.MINUTE, 1 );
        Date upperBound = new Date( cal.getTime().getTime() );

        // one minute tolerance
        assertThat( verificationToken.getExpiryDate() ).isBetween( lowerBound, upperBound );

    }

    @Test
    public void confirmVerificationToken_whenValidToken_thenSucceed() {
        setUpVerificationTokenMocks();

        User user = userService.confirmVerificationToken( "token1" );
        assertThat( user.isEnabled() ).isTrue();

    }

    @Test
    public void confirmVerificationToken_whenInvalidToken_thenThrowTokenException() {
        setUpVerificationTokenMocks();

        try {
            userService.confirmVerificationToken( "tokenBad" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void confirmVerificationToken_whenExpiredToken_thenThrowTokenException() {
        setUpVerificationTokenMocks();
        User user = createUser( 1 );

        try {
            userService.confirmVerificationToken( "token1Expired" );
        } catch (TokenException e) {
            // Expected
            return;
        }
        fail( "Should have thrown TokenException" );
    }

    @Test
    public void updateTermsAndGenesInTaxon_whenUserHasNoGeneOrTerms() {
        User user = createUser( 1 );
        becomeUser( user );
        Taxon taxon = createTaxon( 1 );

        // Mock goService.getRelatedGenes
        Collection<Gene> calculatedGenes = IntStream.range( 101, 110 ).boxed().map(
                nbr -> createGene( nbr, taxon )
        ).collect( Collectors.toList() );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) ).thenReturn( calculatedGenes );

        Collection<GeneOntologyTerm> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTermWithGene( toGOId( nbr ), createGene( nbr, taxon ) )
        ).collect( Collectors.toSet() );

        Map<Gene, TierType> geneTierMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<Gene, Optional<PrivacyLevelType>> privacyLevelMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> Optional.of( PrivacyLevelType.PUBLIC ) ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

    }

    @Test
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTerms() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        Gene geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGene( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1 ) );

        Gene geneWillChangeTier = createGene( 5, taxon );
        GeneOntologyTerm termWillRemain = createTermWithGene( toGOId( geneWillChangeTier.getGeneId() ), geneWillChangeTier );
        user.getUserTerms().add( createUserTerm( 2, termWillRemain, taxon ) );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER3 ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<Gene> calculatedGenes = IntStream.range( 101, 110 ).boxed().map(
                nbr -> createGene( nbr, taxon )
        ).collect( Collectors.toList() );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) ).thenReturn( calculatedGenes );

        Collection<GeneOntologyTerm> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTermWithGene( toGOId( nbr ), createGene( nbr, taxon ) )
        ).collect( Collectors.toSet() );

        Map<Gene, TierType> geneTierMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<Gene, Optional<PrivacyLevelType>> privacyLevelMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> Optional.of( PrivacyLevelType.PUBLIC ) ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

    }

    @Test
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTermsInMultipleTaxons() {
        User user = createUser( 1 );

        Taxon taxon = createTaxon( 1 );
        Gene geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGene( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1 ) );

        Gene geneWillChangeTier = createGene( 5, taxon );
        GeneOntologyTerm termWillRemain = createTermWithGene( toGOId( geneWillChangeTier.getGeneId() ), geneWillChangeTier );
        user.getUserTerms().add( createUserTerm( 2, termWillRemain, taxon ) );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER3 ) );

        // Taxon 2
        Taxon taxon2 = createTaxon( 2 );
        Gene geneOtherTaxon = createGene( 9999, taxon2 );
        GeneOntologyTerm termOtherTaxon = createTermWithGene( toGOId( geneOtherTaxon.getGeneId() ), geneOtherTaxon );
        user.getUserTerms().add( createUserTerm( 3, termOtherTaxon, taxon2 ) );
        user.getUserGenes().put( geneOtherTaxon.getGeneId(), createUserGene( 3, geneOtherTaxon, user, TierType.TIER1 ) );

        Gene geneOtherTaxon2 = createGene( 5555, taxon2 );
        GeneOntologyTerm termOtherTaxon2 = createTermWithGene( toGOId( geneOtherTaxon2.getGeneId() ), geneOtherTaxon2 );
        user.getUserTerms().add( createUserTerm( 4, termOtherTaxon2, taxon2 ) );
        user.getUserGenes().put( geneOtherTaxon2.getGeneId(), createUserGene( 4, geneOtherTaxon2, user, TierType.TIER3 ) );

        becomeUser( user );

        Collection<GeneOntologyTerm> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTermWithGene( toGOId( nbr ), createGene( nbr, taxon ) )
        ).collect( Collectors.toSet() );

        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) )
                .then( i -> {
                    @SuppressWarnings("unchecked") Collection<GeneOntologyTerm> givenTerms = i.getArgumentAt( 0, Collection.class );
                    return givenTerms.stream().flatMap( t -> t.getDirectGenes().stream() ).collect( Collectors.toSet() );
                } );

        Map<Gene, TierType> geneTierMap = Maps.newHashMap( geneWillChangeTier, TierType.TIER1 );


        Map<Gene, Optional<PrivacyLevelType>> privacyLevelMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> Optional.of( PrivacyLevelType.PUBLIC ) ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );
        assertThatUserTermsAreEqualTo( user, taxon2, Sets.newSet( termOtherTaxon, termOtherTaxon2 ) );

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        terms.stream().flatMap( t -> t.getDirectGenes().stream() ).forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        Map<Gene, TierType> expectedGenesTaxon2 = new HashMap<>();
        expectedGenesTaxon2.put( geneOtherTaxon, TierType.TIER1 );
        expectedGenesTaxon2.put( geneOtherTaxon2, TierType.TIER3 );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );
        assertThatUserGenesAreEqualTo( user, taxon2, expectedGenesTaxon2 );

    }

    @Test
    public void updateTermsAndGenesInTaxon_whenManualAndCalculatedGenesOverlap_thenKeepManual() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<Gene> calculatedGenes = IntStream.range( 5, 14 ).boxed().map(
                nbr -> createGene( nbr, taxon )
        ).collect( Collectors.toList() );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) ).thenReturn( calculatedGenes );

        Collection<GeneOntologyTerm> terms = IntStream.range( 1, 10 ).boxed().map(
                nbr -> createTermWithGene( toGOId( nbr ), createGene( nbr, taxon ) )
        ).collect( Collectors.toSet() );

        Map<Gene, TierType> geneTierMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );


        Map<Gene, Optional<PrivacyLevelType>> privacyLevelMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> Optional.of( PrivacyLevelType.PUBLIC ) ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );
    }

    @Test
    public void updateTermsAndGenesInTaxon_whenOldAndNewOverlap_thenRetainIds() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        // These should retain ids so that Hibernate does not delete and re insert rows
        Gene geneOverlapsManual = createGene( 5, taxon );
        GeneOntologyTerm termOverlapsManual = createTermWithGene( toGOId( geneOverlapsManual.getGeneId() ), geneOverlapsManual );
        user.getUserTerms().add( createUserTerm( 1, termOverlapsManual, taxon ) );
        user.getUserGenes().put( geneOverlapsManual.getGeneId(), createUserGene( 1, geneOverlapsManual, user, TierType.TIER3 ) );

        Gene geneOverlapsCalculated = createGene( 105, taxon );
        GeneOntologyTerm termOverlapsCalculated = createTermWithGene( toGOId( geneOverlapsCalculated.getGeneId() ), geneOverlapsCalculated );
        user.getUserTerms().add( createUserTerm( 2, termOverlapsCalculated, taxon ) );
        user.getUserGenes().put( geneOverlapsCalculated.getGeneId(), createUserGene( 2, geneOverlapsCalculated, user, TierType.TIER3 ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<Gene> calculatedGenes = Collections.singleton( createGene( 105, taxon ) );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) ).thenReturn( calculatedGenes );

        Collection<GeneOntologyTerm> terms = Collections.singleton( createTermWithGene( toGOId( 5 ), createGene( 5, taxon ) ) );

        Map<Gene, TierType> geneTierMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, new HashMap<>(), terms );

        // Might as well test this
        assertThatUserTermsAreEqualTo( user, taxon, terms );

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );

        // This is really why we're here
        assertThat( user.getUserTerms().iterator().next().getId() ).isEqualTo( 1 );
        assertThat( user.getUserGenes().values().iterator().next().getId() ).isEqualTo( 1 );
    }

    @Test
    public void updateTermsAndGenesInTaxon_whenUserHasGenesAndTerms_thenUpdateFrequency() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        Gene geneWillBeRemoved = createGene( 999, taxon );
        GeneOntologyTerm termWillBeRemoved = createTermWithGene( toGOId( geneWillBeRemoved.getGeneId() ), geneWillBeRemoved );
        user.getUserTerms().add( createUserTerm( 1, termWillBeRemoved, taxon ) );
        user.getUserGenes().put( geneWillBeRemoved.getGeneId(), createUserGene( 1, geneWillBeRemoved, user, TierType.TIER1 ) );

        Gene geneWillChangeTier = createGene( 5, taxon );
        user.getUserGenes().put( geneWillChangeTier.getGeneId(), createUserGene( 2, geneWillChangeTier, user, TierType.TIER2 ) );

        UserTerm termWillUpdateFrequency = createUserTerm( 3,
                createTermWithGene( toGOId( geneWillChangeTier.getGeneId() ), geneWillBeRemoved, geneWillChangeTier ),
                taxon );
        // Should have frequency of 2
        termWillUpdateFrequency.setFrequency( 2 );
        user.getUserTerms().add( termWillUpdateFrequency );

        becomeUser( user );

        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.any() ) ).thenReturn( Collections.emptySet() );

        Map<Gene, TierType> geneTierMap = Maps.newHashMap( geneWillChangeTier, TierType.TIER1 );
        Map<Gene, Optional<PrivacyLevelType>> genePrivacyLevelTypeMap = Maps.newHashMap(geneWillChangeTier, Optional.of(PrivacyLevelType.PRIVATE) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, genePrivacyLevelTypeMap, Sets.newSet( termWillUpdateFrequency) );

        assertThat( user.getUserTerms() ).hasSize( 1 );
        assertThat( user.getUserTerms().iterator().next() ).isEqualTo( termWillUpdateFrequency );
        assertThat( user.getUserTerms().iterator().next().getFrequency() ).isEqualTo( 1 );

    }

    @Test
    public void updateTermsAndGenesInTaxon_whenTermsOverlapInDifferentSpecies_thenKeepBothTerms() {
        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Taxon taxon2 = createTaxon( 2 );

        // These should retain ids so that Hibernate does not delete and re insert rows
        Gene geneInTaxon1 = createGene( 5, taxon );
        GeneOntologyTerm termInTaxon1 = createTermWithGene( toGOId( geneInTaxon1.getGeneId() ), geneInTaxon1 );
        user.getUserTerms().add( createUserTerm( 1, termInTaxon1, taxon ) );
        user.getUserGenes().put( geneInTaxon1.getGeneId(), createUserGene( 1, geneInTaxon1, user, TierType.TIER3 ) );

        Gene geneInTaxon2 = createGene( 105, taxon2 );
        GeneOntologyTerm termInTaxon2 = createTermWithGene( toGOId( geneInTaxon2.getGeneId() ), geneInTaxon2 );
        user.getUserTerms().add( createUserTerm( 2, termInTaxon2, taxon2 ) );
        user.getUserGenes().put( geneInTaxon2.getGeneId(), createUserGene( 2, geneInTaxon2, user, TierType.TIER3 ) );

        becomeUser( user );

        // Mock goService.getRelatedGenes
        Collection<Gene> calculatedGenes = Collections.singleton( createGene( 205, taxon ) );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.eq( taxon ) ) ).thenReturn( calculatedGenes );
        Mockito.when( goService.getGenes( Mockito.anyCollectionOf( GeneOntologyTerm.class ), Mockito.eq( taxon2 ) ) ).thenReturn( Collections.emptySet() );

        // Attempting to add term to taxon 1 that is already present in taxon 2
        Collection<GeneOntologyTerm> terms = Collections.singleton( createTermWithGene( toGOId( 105 ), createGene( 1005, taxon ) ) );

        Map<Gene, TierType> geneTierMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> TierType.TIER1 ) );

        Map<Gene, Optional<PrivacyLevelType>> privacyLevelMap = terms.stream()
                .flatMap( t -> t.getDirectGenes().stream() )
                .collect( Collectors.toMap( Function.identity(), g -> Optional.of(PrivacyLevelType.PRIVATE) ) );

        userService.updateTermsAndGenesInTaxon( user, taxon, geneTierMap, privacyLevelMap, terms );

        // Why we are here
        assertThatUserTermsAreEqualTo( user, taxon, terms );
        assertThatUserTermsAreEqualTo( user, taxon2, Collections.singleton( termInTaxon2 ));

        Map<Gene, TierType> expectedGenes = new HashMap<>( geneTierMap );
        calculatedGenes.forEach( g -> expectedGenes.putIfAbsent( g, TierType.TIER3 ) );

        assertThatUserGenesAreEqualTo( user, taxon, expectedGenes );
        assertThatUserGenesAreEqualTo( user, taxon2, Maps.newHashMap( geneInTaxon2, TierType.TIER3 ) );

    }

    private void assertThatUserTermsAreEqualTo( User user, Taxon taxon, Collection<GeneOntologyTerm> terms ) {
        assertThat( user.getUserTerms().stream().filter( t -> t.getTaxon().equals( taxon ) ).count() ).isEqualTo( terms.size() );
        assertThat( user.getUserTerms().stream().filter( t -> t.getTaxon().equals( taxon ) ).map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) )
                .isEqualTo( terms.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toSet() ) );
    }

    private void assertThatUserGenesAreEqualTo( User user, Taxon taxon, Map<Gene, TierType> expectedGenes ) {
        assertThat( user.getGenesByTaxon( taxon ).stream().map(e -> (Gene) e).collect(Collectors.toSet()) )
                .containsExactlyElementsOf( expectedGenes.keySet() );
        expectedGenes.forEach( ( g, tier ) -> assertThat( user.getUserGenes().get( g.getGeneId() ).getTier() ).isEqualTo( tier ) );
    }


    @Test
    public void recommendTerms_thenReturnBestResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );
    }

    @Test
    public void recommendTerms_whenMinSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 12, -1, -1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );

        found = userService.recommendTerms( user, taxon, 20, -1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenMaxSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, 12, -1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );

        found = userService.recommendTerms( user, taxon, -1, 1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenFrequencyLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, -1, 3 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 1 ), toGOId( 7 ), toGOId( 8 ) );

        found = userService.recommendTerms( user, taxon, -1, -1, 4 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenFrequencyLimitedAndSizeLimited_thenReturnBestLimitedResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 11, 12, 2 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );

        found = userService.recommendTerms( user, taxon, 1, 11, 2 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenRedundantTerms_thenReturnOnlyMostSpecific() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, 11, 11, 1 );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 2 ), toGOId( 3 ), toGOId( 5 ), toGOId( 99 ) );

        found = userService.recommendTerms( user, taxon, 1, 11, 2 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenUserHasSomeTopTerms_thenReturnNewBestResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        user.getUserTerms().add( new UserTerm( createTerm( toGOId( 1 ) ), taxon, null ) );

        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 7 ), toGOId( 8 ) );
    }

    @Test
    public void recommendTerms_whenUserHasAllTopTerms_thenReturnNextBestResultsOnly() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );

        user.getUserTerms().add( new UserTerm( createTerm( toGOId( 1 ) ), taxon, null ) );
        user.getUserTerms().add( new UserTerm( createTerm( toGOId( 7 ) ), taxon, null ) );
        user.getUserTerms().add( new UserTerm( createTerm( toGOId( 8 ) ), taxon, null ) );

        Collection<UserTerm> found = userService.recommendTerms( user, taxon );
        assertThat( found.stream().map( GeneOntologyTerm::getGoId ).collect( Collectors.toList() ) ).containsExactlyInAnyOrder( toGOId( 0 ), toGOId( 4 ), toGOId( 6 ) );
    }

    @Test
    public void recommendTerms_whenUserHasNoGenes_thenReturnEmpty() {
        Map<GeneOntologyTerm, Long> empyFMap = new HashMap<>();
        Mockito.when( goService.termFrequencyMap( Mockito.anyCollectionOf( Gene.class ) ) ).thenReturn( empyFMap );

        User user = createUser( 1 );
        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, taxon, -1, -1, -1 );
        assertThat( found ).isEmpty();
    }

    @Test
    public void recommendTerms_whenUserNull_thenReturnNull() {
        setUpRecomendTermsMocks();

        Taxon taxon = createTaxon( 1 );
        Collection<UserTerm> found = userService.recommendTerms( null, taxon, -1, -1, -1 );
        assertThat( found ).isNull();
    }

    @Test
    public void recommendTerms_whenTaxonNull_thenReturnNull() {
        setUpRecomendTermsMocks();

        User user = createUser( 1 );
        Collection<UserTerm> found = userService.recommendTerms( user, null, -1, -1, -1 );
        assertThat( found ).isNull();
    }


}
