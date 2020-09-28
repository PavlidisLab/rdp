package ubc.pavlab.rdp.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;

import java.util.Collections;

import static org.mockito.BDDMockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createUser;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
public class AdminControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean(name = "taxonService")
    private TaxonService taxonService;

    @MockBean(name = "userService")
    private UserService userService;

    @MockBean(name = "applicationSettings")
    private ApplicationSettings applicationSettings;

    @MockBean
    ApplicationSettings.PrivacySettings privacySettings;

    @MockBean
    private SiteSettings siteSettings;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private FormattingConversionService conversionService;

    private class UserIdToUserConverter implements Converter<String, User> {

        @Override
        public User convert( String s ) {
            return userService.findUserById( Integer.parseInt( s ) );
        }
    }

    @Before
    public void setUp() {
        when( applicationSettings.getPrivacy() ).thenReturn( privacySettings );
        conversionService.addConverter( new UserIdToUserConverter() );
    }

    @Test
    public void givenNotLoggedIn_whenDeleteUser_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );
        when( userService.findUserById( eq( 1 ) ) ).thenReturn( user );

        mvc.perform( get( "/admin/users/{userId}", user.getId() )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedInAsUser_whenDeleteUser_thenReturn403()
            throws Exception {

        User me = createUser( 1 );

        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( get( "/admin/users/{userId}", me.getId() )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedInAsAdmin_whenDeleteUser_thenSucceed()
            throws Exception {

        User me = createUser( 1 );

        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( delete( "/admin/users/{userId}", me.getId() )
                .param( "email", me.getEmail() ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/admin/users" ) );

        verify( userService ).delete( me );
    }

    @Test
    @WithMockUser(roles = { "ADMIN" })
    public void givenLoggedInAsAdmin_whenDeleteUserWithWrongConfirmationEmail_thenReturnBadRequest() throws Exception {
        User me = createUser( 1 );

        when( taxonService.findByActiveTrue() ).thenReturn( Collections.emptySet() );
        when( userService.findUserById( eq( 1 ) ) ).thenReturn( me );

        mvc.perform( delete( "/admin/users/{userId}", me.getId() )
                .param( "email", "123@example.com" ) )
                .andExpect( status().isBadRequest() )
                .andExpect( view().name( "admin/user" ) );

        verify( userService, never() ).delete( me );
    }
}
