package ubc.pavlab.rdp.controllers;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.security.Permissions;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

@WebMvcTest(MainController.class)
@TestPropertySource("classpath:application.properties")
@Import({ ApplicationSettings.class, SiteSettings.class })
public class MainControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean(name = "userService")
    private UserService userService;

    @MockBean(name = "taxonService")
    private TaxonService taxonService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Test
    public void getIndex_redirect3xx() throws Exception {
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.SEARCH ) ) ).thenReturn( true );
        mvc.perform( get( "/" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/search" ) );
    }

    @Test
    public void getIndex_withoutPublicSearch_redirect3xx() throws Exception {
        mvc.perform( get( "/" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "http://localhost/login" ) );
    }

    @Test
    @WithMockUser
    public void getIndex_withUser_redirect3xx() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        when( permissionEvaluator.hasPermission( any(), isNull(), eq( Permissions.SEARCH ) ) ).thenReturn( true );
        mvc.perform( get( "/" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/user/home" ) );
    }

    @Test
    public void termsOfService() throws Exception {
        when( ontologyMessageSource.getMessage( eq( "rdp.terms-of-service" ), any(), isNull(), any() ) ).thenReturn( "Do what you please." );
        mvc.perform( get( "/terms-of-service" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "terms-of-service" ) )
                .andExpect( content().string( containsString( "Do what you please." ) ) );
        verify( ontologyMessageSource ).getMessage( eq( "rdp.terms-of-service" ), eq( new String[]{ "RDP", "Rare Disease Project" } ), isNull(), any() );
    }

    @Test
    public void termsOfService_whenMessageIsMissing() throws Exception {
        when( ontologyMessageSource.getMessage( any(), any(), isNull(), any() ) ).thenThrow( NoSuchMessageException.class );
        mvc.perform( get( "/terms-of-service" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "message", "No terms of service document is setup for this registry." ) );
        verify( ontologyMessageSource ).getMessage( eq( "rdp.terms-of-service" ), eq( new String[]{ "RDP", "Rare Disease Project" } ), isNull(), any() );
    }

    @Test
    public void privacyPolicy_whenMessageIsMissing() throws Exception {
        when( ontologyMessageSource.getMessage( any(), any(), isNull(), any() ) ).thenThrow( NoSuchMessageException.class );
        mvc.perform( get( "/privacy-policy" ) )
                .andExpect( status().isNotFound() )
                .andExpect( view().name( "error/404" ) )
                .andExpect( model().attribute( "message", "No privacy policy is setup for this registry." ) );
        verify( ontologyMessageSource ).getMessage( eq( "rdp.privacy-policy" ), eq( new String[]{ "RDP", "Rare Disease Project" } ), isNull(), any() );
    }

    @Test
    @WithMockUser
    public void getTimeout_withUser_returnNoContent() throws Exception {
        User user = createUser( 1 );
        when( userService.findCurrentUser() ).thenReturn( user );
        long timeoutInSeconds = 0L;
        mvc.perform( get( "/gettimeout" ) )
                .andExpect( status().isNoContent() )
                .andExpect( cookie().value( "serverTime", asDouble( closeTo( System.currentTimeMillis(), 100 ) ) ) )
                .andExpect( cookie().path( "serverTime", "/" ) )
                .andExpect( cookie().secure( "serverTime", false ) )
                .andExpect( cookie().httpOnly( "serverTime", false ) )
                .andExpect( cookie().value( "sessionExpiry", asDouble( closeTo( System.currentTimeMillis() + 1000L * ( timeoutInSeconds - 60L ), 100 ) ) ) )
                .andExpect( cookie().path( "sessionExpiry", "/" ) )
                .andExpect( cookie().secure( "sessionExpiry", false ) )
                .andExpect( cookie().httpOnly( "sessionExpiry", false ) );
    }

    private static FeatureMatcher<String, Double> asDouble( Matcher<Double> matcher ) {
        return new FeatureMatcher<String, Double>( matcher, "", "" ) {
            @Override
            protected Double featureValueOf( String s ) {
                return (double) Long.parseLong( s );
            }
        };
    }

    @Test
    public void getTimeout_withoutUser_redirect3xx() throws Exception {
        mvc.perform( get( "/gettimeout" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "http://localhost/login" ) );
    }
}
