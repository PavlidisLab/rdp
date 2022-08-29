package ubc.pavlab.rdp.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.services.UserPrivacyService;
import ubc.pavlab.rdp.services.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@RunWith(SpringRunner.class)
public class PermissionEvaluatorImplTest {

    @TestConfiguration
    static class PermissionEvaluatorImplTestContextConfiguration {

        @Bean
        public PermissionEvaluator permissionEvaluator() {
            return new PermissionEvaluatorImpl();
        }
    }

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private UserService userService;

    @MockBean
    private UserPrivacyService privacyService;

    /* fixture */
    private UserContent userContent;

    @Before
    public void setUp() {
        userContent = mock( UserContent.class );
    }

    @Test
    public void hasPermission_whenUserIsAnonymous_thenCallAppropriatePrivacyServiceMethod() {
        AnonymousAuthenticationToken auth = mock( AnonymousAuthenticationToken.class );

        permissionEvaluator.hasPermission( auth, null, Permissions.INTERNATIONAL_SEARCH );
        verify( privacyService ).checkUserCanSearch( null, true );

        permissionEvaluator.hasPermission( auth, null, Permissions.SEARCH );
        verify( privacyService ).checkUserCanSearch( null, false );

        permissionEvaluator.hasPermission( auth, userContent, Permissions.READ );
        verify( privacyService ).checkUserCanSee( null, userContent );

        permissionEvaluator.hasPermission( auth, userContent, Permissions.UPDATE );
        verify( privacyService ).checkUserCanUpdate( null, userContent );
    }

    @Test
    public void hasPermission_whenUserIsLoggedIn_thenCallAppropriatePrivacyServiceMethod() {
        Authentication auth = mock( Authentication.class );
        User user = createUser( 5 );
        UserPrinciple userPrinciple = mock( UserPrinciple.class );
        when( userPrinciple.getId() ).thenReturn( 5 );
        when( auth.getPrincipal() ).thenReturn( userPrinciple );
        when( userService.findUserByIdNoAuth( 5 ) ).thenReturn( user );

        permissionEvaluator.hasPermission( auth, null, Permissions.SEARCH );
        verify( userService ).findUserByIdNoAuth( 5 );
        verify( privacyService ).checkUserCanSearch( user, false );

        permissionEvaluator.hasPermission( auth, userContent, Permissions.READ );
        verify( privacyService ).checkUserCanSee( user, userContent );

        permissionEvaluator.hasPermission( auth, userContent, Permissions.UPDATE );
        verify( privacyService ).checkUserCanUpdate( user, userContent );
    }

    @Test
    public void hasPermission_whenUserContentIsNull_thenReturnTrue() {
        AnonymousAuthenticationToken auth = mock( AnonymousAuthenticationToken.class );
        assertThat( permissionEvaluator.hasPermission( auth, null, Permissions.READ ) ).isTrue();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hasPermission_whenPermissionIsUnknown_thenRaiseException() {
        AnonymousAuthenticationToken auth = mock( AnonymousAuthenticationToken.class );
        permissionEvaluator.hasPermission( auth, null, "what?" );
    }
}