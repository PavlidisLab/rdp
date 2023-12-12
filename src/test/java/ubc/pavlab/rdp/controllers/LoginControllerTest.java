package ubc.pavlab.rdp.controllers;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Errors;
import ubc.pavlab.rdp.exception.TokenDoesNotMatchEmailException;
import ubc.pavlab.rdp.exception.TokenNotFoundException;
import ubc.pavlab.rdp.model.Profile;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.services.ExpiredTokenException;
import ubc.pavlab.rdp.services.PrivacyService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.validation.EmailValidator;
import ubc.pavlab.rdp.validation.Recaptcha;
import ubc.pavlab.rdp.validation.RecaptchaValidator;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@WebMvcTest(LoginController.class)
@TestPropertySource(value = "classpath:application.properties", properties = {
        "rdp.site.contact-email=support@example.com"
})
@Import({ ApplicationSettings.class, SiteSettings.class })
public class LoginControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private PrivacyService privacyService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private FormattingConversionService formattingConversionService;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @MockBean
    private RecaptchaValidator recaptchaValidator;

    @MockBean
    private EmailValidator emailValidator;

    @BeforeEach
    public void setUp() {
        when( privacyService.getDefaultPrivacyLevel() ).thenReturn( PrivacyLevelType.PRIVATE );
        when( emailValidator.supports( String.class ) ).thenReturn( true );
    }

    @Test
    public void login_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "login" ) )
                .andExpect( content().contentTypeCompatibleWith( MediaType.TEXT_HTML ) )
                .andExpect( content().string( containsString( "support@example.com" ) ) );
    }

    @Test
    @WithMockUser
    public void login_whenLoggedIn_thenForward() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( forwardedUrl( "/" ) );
    }

    @Test
    @WithMockUser
    public void login_whenAlreadyLoggedIn_thenRedirect3xx() throws Exception {
        mvc.perform( get( "/login" ) )
                .andExpect( status().isOk() )
                .andExpect( forwardedUrl( "/" ) );
    }

    @Test
    public void register_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/registration" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "registration" ) )
                .andExpect( model().attribute( "user", new User() ) );
        when( userService.create( any() ) ).thenAnswer( answer -> answer.getArgument( 0, User.class ) );
        mvc.perform( post( "/registration" )
                        .header( "X-Forwarded-For", "127.0.0.1", "10.0.0.2" )
                        .param( "profile.name", "Bob" )
                        .param( "profile.lastName", "Smith" )
                        .param( "email", "bob@example.com" )
                        .param( "password", "123456" )
                        .param( "passwordConfirm", "123456" )
                        .param( "g-recaptcha-response", "1234" )
                        .param( "id", "27" ) ) // this field is ignored
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/login" ) );
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass( User.class );
        verify( userService ).create( captor.capture() );
        verify( userService ).createVerificationTokenForUser( eq( captor.getValue() ), any() );
        assertThat( captor.getValue() ).satisfies( user -> {
            assertThat( user.getId() ).isNull();
            assertThat( user.getEmail() ).isEqualTo( "bob@example.com" );
            assertThat( user.isEnabled() ).isFalse();
            assertThat( user.getAnonymousId() ).isNull();
        } );
        ArgumentCaptor<Recaptcha> recaptchaCaptor = ArgumentCaptor.forClass( Recaptcha.class );
        verify( recaptchaValidator ).validate( recaptchaCaptor.capture(), any() );
        assertThat( recaptchaCaptor.getValue() ).satisfies( r -> {
            assertThat( r.getResponse() ).isEqualTo( "1234" );
            assertThat( r.getRemoteIp() ).isEqualTo( "127.0.0.1" );
        } );
    }

    @Test
    public void register_whenEmailDomainIsNotAccepted_thenProduceHelpfulMessage() throws Exception {
        doAnswer( a -> {
            a.getArgument( 1, Errors.class ).rejectValue( null, "EmailValidator.domainNotAllowed" );
            return null;
        } ).when( emailValidator ).validate( eq( "bob@example.com" ), any() );
        when( emailValidator.supports( String.class ) ).thenReturn( true );
        mvc.perform( post( "/registration" )
                        .param( "profile.name", "Bob" )
                        .param( "profile.lastName", "Smith" )
                        .param( "email", "bob@example.com" )
                        .param( "password", "123456" )
                        .param( "passwordConfirm", "123456" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( model().attribute( "domainNotAllowed", true ) )
                .andExpect( model().attribute( "domainNotAllowedFrom", "bob@example.com" ) )
                .andExpect( model().attribute( "domainNotAllowedSubject", "Register with an email address from example.com" ) )
                .andExpect( model().attribute( "domainNotAllowedBody", containsString( "bob@example.com" ) ) )
                .andExpect( xpath( "//a[starts-with(@href, 'mailto:')]/@href" ).string( Matchers.startsWith( "mailto:support@example.com?from=bob@example.com&subject=Register" ) ) );
    }

    @Test
    @Disabled("I have absolutely no idea why this converter does not work anymore. See https://github.com/PavlidisLab/rdp/issues/171 for details.")
    public void register_whenEmailIsUsedButNotEnabled_thenResendConfirmation() throws Exception {
        User user = User.builder( new Profile() )
                .email( "foo@example.com" )
                .enabled( false )
                .build();
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( user );

        //noinspection Convert2Lambda
        formattingConversionService.addConverter( new Converter<Object, User>() {
            @Override
            public User convert( Object o ) {
                return User.builder( Profile.builder().name( "Foo" ).build() )
                        .email( "foo@example.com" )
                        .build();
            }
        } );

        mvc.perform( post( "/registration" )
                        .locale( Locale.getDefault() )
                        .param( "user.profile.name", "Foo" )
                        .param( "user.email", "foo@example.com" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "registration" ) )
                .andExpect( model().attribute( "user", new User() ) );

        verify( userService ).createVerificationTokenForUser( user, Locale.getDefault() );
    }

    @Test
    public void resendConfirmation_thenReturnSuccess() throws Exception {
        mvc.perform( get( "/resendConfirmation" )
                        .locale( Locale.getDefault() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "resendConfirmation" ) );

        User user = createUser( 1 );
        user.setEnabled( false );
        when( userService.findUserByEmailNoAuth( "foo@example.com" ) ).thenReturn( user );

        mvc.perform( post( "/resendConfirmation" )
                        .locale( Locale.getDefault() )
                        .param( "email", "foo@example.com" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "resendConfirmation" ) );

        verify( userService ).findUserByEmailNoAuth( "foo@example.com" );
        verify( userService ).createVerificationTokenForUser( user, Locale.getDefault() );
    }

    @Test
    public void registrationConfirm_thenReturnSuccess() throws Exception {
        User user = createUser( 1 );
        when( userService.confirmVerificationToken( eq( "1234" ), any() ) ).thenReturn( user );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/login" ) )
                .andExpect( flash().attributeExists( "message" ) )
                .andExpect( flash().attribute( "error", (Object) null ) );
    }

    @Test
    public void registrationConfirm_whenTokenDoesNotExist_thenRedirectToLoginWithMessage() throws Exception {
        when( userService.confirmVerificationToken( eq( "1234" ), any() ) ).thenThrow( TokenNotFoundException.class );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/login" ) )
                .andExpect( flash().attributeExists( "message" ) )
                .andExpect( flash().attribute( "error", (Object) null ) );
    }

    @Test
    public void registrationConfirm_whenTokenIsExpired_thenRedirectToResendConfirmation() throws Exception {
        when( userService.confirmVerificationToken( eq( "1234" ), any() ) ).thenThrow( ExpiredTokenException.class );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/resendConfirmation" ) )
                .andExpect( flash().attributeExists( "message" ) )
                .andExpect( flash().attribute( "error", (Object) null ) );
    }

    @Test
    public void registrationConfirm_whenTokenIsInvalid_thenReturn400() throws Exception {
        when( userService.confirmVerificationToken( eq( "1234" ), any() ) ).thenThrow( TokenDoesNotMatchEmailException.class );
        mvc.perform( get( "/registrationConfirm" )
                        .param( "token", "1234" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "registrationConfirm" ) )
                .andExpect( model().attributeExists( "message" ) )
                .andExpect( model().attribute( "error", Boolean.TRUE ) );
    }
}
