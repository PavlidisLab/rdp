package ubc.pavlab.rdp.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.PasswordReset;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.services.EmailService;
import ubc.pavlab.rdp.services.UserDetailsServiceImpl;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@RunWith(SpringRunner.class)
@WebMvcTest(PasswordController.class)
@Import(WebSecurityConfig.class)
public class PasswordControllerTest {

    @TestConfiguration
    static class PasswordControllerTestContextConfiguration {

        @Bean
        public UserDetailsService userDetailsService() {
            return new UserDetailsServiceImpl();
        }

        @Bean
        public BCryptPasswordEncoder bCryptPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockBean(name = "siteSettings")
    private SiteSettings siteSettings;

    @MockBean
    private UserService userService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Test
    public void forgotPassword_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/forgotPassword" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "forgotPassword" ) );

        User user = createUser( 1 );
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( user );

        mvc.perform( post( "/forgotPassword" )
                        .locale( Locale.getDefault() )
                        .param( "email", "foo@example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "forgotPassword" ) )
                .andExpect( model().attribute( "error", false ) );

        verify( userService ).createPasswordResetTokenForUser( eq( user ), eq( Locale.getDefault() ) );
    }

    @Test
    public void forgotPassword_whenUserDoesNotExist_thenReturnNotFound() throws Exception {
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( null );
        mvc.perform( post( "/forgotPassword" )
                .param( "email", "foo@example.com" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "forgotPassword" ) );
    }

    @Test
    public void updatePassword_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.findUserByIdNoAuth( 1 ) ).thenReturn( user );
        mvc.perform( get( "/updatePassword" )
                .param( "id", "1" )
                .param( "token", "1234" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "updatePassword" ) );

        verify( userService ).verifyPasswordResetToken( 1, "1234" );

        mvc.perform( post( "/updatePassword" )
                .param( "id", "1" )
                .param( "token", "1234" )
                .param( "newPassword", "123456" )
                .param( "passwordConfirm", "123456" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/user/home" ) );

        verify( userService ).changePasswordByResetToken( 1, "1234", new PasswordReset( "123456", "123456" ) );
    }

    @Test
    public void changePasswordByResetToken_whenNewPasswordIsTooShort_thenThrowValidationException() throws Exception {
        mvc.perform( post( "/updatePassword" )
                .param( "id", "1" )
                .param( "token", "1234" )
                .param( "newPassword", "12345" )
                .param( "passwordConfirm", "12345" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "updatePassword" ) );
    }
}
