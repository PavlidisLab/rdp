package ubc.pavlab.rdp.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit4.SpringRunner;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.UserPrinciple;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;

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
    PermissionEvaluator permissionEvaluator;

    @MockBean
    UserService userService;

    @MockBean
    RoleRepository roleRepository;

    @MockBean
    PrivacyService privacyService;

    Authentication auth;

    UserContent userContent;

    @Before
    public void setUp() {
        auth = mock( Authentication.class );
        userContent = mock( UserContent.class );
    }

    @Test
    public void hasPermission_whenUserIsAnonymous_thenCallAppropriatePrivacyServiceMethod() {
        when( auth.getPrincipal() ).thenReturn( "anonymousUser" );

        permissionEvaluator.hasPermission( auth, null, "international-search" );
        verify( privacyService ).checkUserCanSearch( null, true );

        permissionEvaluator.hasPermission( auth, null, "search" );
        verify( privacyService ).checkUserCanSearch( null, false );

        permissionEvaluator.hasPermission( auth, userContent, "read" );
        verify( privacyService ).checkUserCanSee( null, userContent );

        permissionEvaluator.hasPermission( auth, userContent, "update" );
        verify( privacyService ).checkUserCanUpdate( null, userContent );
    }

    @Test
    public void hasPermission_whenUserIsLoggedIn_thenCallAppropriatePrivacyServiceMethod() {
        User user = createUser( 5 );
        UserPrinciple userPrinciple = mock( UserPrinciple.class );
        when( userPrinciple.getId() ).thenReturn( 5 );
        when( auth.getPrincipal() ).thenReturn( userPrinciple );
        when( userService.findUserByIdNoAuth( 5 ) ).thenReturn( user );

        permissionEvaluator.hasPermission( auth, null, "search" );
        verify( userService ).findUserByIdNoAuth( 5 );
        verify( privacyService ).checkUserCanSearch( user, false );

        permissionEvaluator.hasPermission( auth, userContent, "read" );
        verify( privacyService ).checkUserCanSee( user, userContent );

        permissionEvaluator.hasPermission( auth, userContent, "update" );
        verify( privacyService ).checkUserCanUpdate( user, userContent );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hasPermission_whenUserContentIsNull_thenRaiseException() {
        when( auth.getPrincipal() ).thenReturn( "anonymousUser" );
        permissionEvaluator.hasPermission( auth, null, "read" );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hasPermission_whenPermissionIsUnknown_thenRaiseException() {
        when( auth.getPrincipal() ).thenReturn( "anonymousUser" );
        permissionEvaluator.hasPermission( auth, null, "what?" );
    }
}