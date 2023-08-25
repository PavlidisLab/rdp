package ubc.pavlab.rdp.security.authentication;

import org.junit.Test;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.Authentication;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

public class TokenBasedAuthenticationManagerTest {

    @Test
    public void test() {
        UserService userService = mock( UserService.class );
        ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
        when( applicationSettings.getIsearch() ).thenReturn( mock( ApplicationSettings.InternationalSearchSettings.class ) );
        when( applicationSettings.getIsearch().getUserId() ).thenReturn( 1 );
        when( applicationSettings.getIsearch().getAuthTokens() ).thenReturn( Collections.singletonList( "test" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );
        new TokenBasedAuthenticationManager( userService, applicationSettings );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_whenUserIdIsNotSetButAuthTokensAre_thenRaiseIllegalArgumentException() {
        UserService userService = mock( UserService.class );
        ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
        when( applicationSettings.getIsearch() ).thenReturn( mock( ApplicationSettings.InternationalSearchSettings.class ) );
        when( applicationSettings.getIsearch().getUserId() ).thenReturn( null );
        when( applicationSettings.getIsearch().getAuthTokens() ).thenReturn( Collections.singletonList( "test" ) );
        new TokenBasedAuthenticationManager( userService, applicationSettings );
    }

    @Test
    public void test_whenUserIdIsSetButNoAuthTokensAre() {
        UserService userService = mock( UserService.class );
        ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
        when( applicationSettings.getIsearch() ).thenReturn( mock( ApplicationSettings.InternationalSearchSettings.class ) );
        when( applicationSettings.getIsearch().getUserId() ).thenReturn( 1 );
        when( applicationSettings.getIsearch().getAuthTokens() ).thenReturn( Collections.emptyList() );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );
        new TokenBasedAuthenticationManager( userService, applicationSettings );
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_whenRemoteUserDoesNotExist() {
        UserService userService = mock( UserService.class );
        ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
        when( applicationSettings.getIsearch() ).thenReturn( mock( ApplicationSettings.InternationalSearchSettings.class ) );
        when( applicationSettings.getIsearch().getUserId() ).thenReturn( 1 );
        when( applicationSettings.getIsearch().getAuthTokens() ).thenReturn( Collections.singletonList( "test" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.empty() );
        new TokenBasedAuthenticationManager( userService, applicationSettings );
    }

    @Test
    public void authenticate_whenRemoteUserIsRemovedAtRuntime() {
        UserService userService = mock( UserService.class );
        ApplicationSettings applicationSettings = mock( ApplicationSettings.class );
        when( applicationSettings.getIsearch() ).thenReturn( mock( ApplicationSettings.InternationalSearchSettings.class ) );
        when( applicationSettings.getIsearch().getUserId() ).thenReturn( 1 );
        when( applicationSettings.getIsearch().getAuthTokens() ).thenReturn( Collections.singletonList( "test" ) );
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.of( createUser( 1 ) ) );
        TokenBasedAuthenticationManager manager = new TokenBasedAuthenticationManager( userService, applicationSettings );

        // remote user was removed
        when( userService.getRemoteSearchUser() ).thenReturn( Optional.empty() );

        Authentication auth = mock( Authentication.class );
        when( auth.getPrincipal() ).thenReturn( "test" );
        assertThatThrownBy( () -> manager.authenticate( auth ) )
                .isInstanceOf( InternalAuthenticationServiceException.class );
    }
}