package ubc.pavlab.rdp.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.Role;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.PrivacyServiceImpl;
import ubc.pavlab.rdp.services.UserPrivacyService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.*;

@RunWith(SpringRunner.class)
public class PrivacyServiceImplTest {

    @TestConfiguration
    static class PrivacyServiceImplTestContextConfiguration {

        @Bean
        public UserPrivacyService privacyService() {
            return new UserPrivacyService();
        }
    }

    @Autowired
    private UserPrivacyService privacyService;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private ApplicationSettings.PrivacySettings privacySettings;

    @MockBean
    private ApplicationSettings.InternationalSearchSettings iSearchSettings;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private UserService userService;

    private User user;

    private User otherUser;

    private User adminUser;

    private User serviceAccountUser;

    @Before
    public void setUp() {
        Role roleAdmin = createRole( 3, "ROLE_ADMIN" );
        Role roleServiceAccount = createRole( 4, "ROLE_SERVICE_ACCOUNT" );
        user = createUser( 1 );
        user.setEnabled( true );
        user.setEnabledAt( Timestamp.from( Instant.now() ) );
        otherUser = createUser( 2 );
        otherUser.setEnabled( true );
        otherUser.setEnabledAt( Timestamp.from( Instant.now() ) );
        adminUser = createUserWithRoles( 3, roleAdmin );
        serviceAccountUser = createUserWithRoles( 4, roleServiceAccount );
        when( roleRepository.findByRole( "ROLE_ADMIN" ) ).thenReturn( roleAdmin );
        when( roleRepository.findByRole( "ROLE_SERVICE_ACCOUNT" ) ).thenReturn( roleServiceAccount );
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        when( applicationSettings.getIsearch() ).thenReturn( iSearchSettings );
        when( privacySettings.isRegisteredSearch() ).thenReturn( true );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.empty() );
    }

    @Test
    public void checkUserCanSearch_thenReturnTrue() {
        assertThat( privacyService.checkUserCanSearch( user, false ) ).isTrue();
    }

    @Test
    public void checkUserCanSearchInternationally_whenInternationalSearchIsDisabled_thenReturnFalse() {
        assertThat( privacyService.checkUserCanSearch( user, true ) ).isFalse();
    }

    @Test
    public void checkUserCanSee_whenLookingAtHisOwnContent_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( user ) );
        assertThat( privacyService.checkUserCanSee( user, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenLookingAtPublicContent_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.PUBLIC );
        assertThat( privacyService.checkUserCanSee( user, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenLookingAtPrivateContent_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.PRIVATE );
        assertThat( privacyService.checkUserCanSee( user, userContent ) ).isFalse();
    }

    @Test
    public void checkUserCanSee_whenLookingAtSharedContent_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.SHARED );
        assertThat( privacyService.checkUserCanSee( user, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenAnonymousUserLookingAtPublicContent_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.PUBLIC );
        assertThat( privacyService.checkUserCanSee( null, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenAnonymousUserLookingAtSharedContent_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.SHARED );
        assertThat( privacyService.checkUserCanSee( null, userContent ) ).isFalse();
    }

    @Test
    public void checkUserCanSee_whenAdminUserLookingAtSharedContent_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.SHARED );
        assertThat( privacyService.checkUserCanSee( adminUser, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenAdminUserLookingAtPrivateContent_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.SHARED );
        assertThat( privacyService.checkUserCanSee( adminUser, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanUpdate_whenUpdatingHisOwnContent_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( user ) );
        assertThat( privacyService.checkUserCanUpdate( user, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanUpdate_whenUpdatingOtherUserContent_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        assertThat( privacyService.checkUserCanUpdate( user, userContent ) ).isFalse();
    }

    @Test
    public void checkUserCanUpdate_whenUpdatingOtherUserContentWithAdminRole_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        assertThat( privacyService.checkUserCanUpdate( adminUser, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanSearch_whenUserIsServiceAccount_thenReturnTrue() {
        assertThat( privacyService.checkUserCanSearch( serviceAccountUser, false ) ).isTrue();
    }

    @Test
    public void checkUserCanSee_whenUserIsServiceAccount_thenReturnTrue() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.PRIVATE );
        assertThat( privacyService.checkUserCanSee( serviceAccountUser, userContent ) ).isTrue();
    }

    @Test
    public void checkUserCanUpdate_whenUserIsServiceAccount_thenReturnFalse() {
        UserContent userContent = mock( UserContent.class );
        when( userContent.getOwner() ).thenReturn( Optional.of( otherUser ) );
        when( userContent.getEffectivePrivacyLevel() ).thenReturn( PrivacyLevelType.PRIVATE );
        assertThat( privacyService.checkUserCanUpdate( serviceAccountUser, userContent ) ).isFalse();
    }
}
