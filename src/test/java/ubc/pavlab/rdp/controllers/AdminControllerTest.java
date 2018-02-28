package ubc.pavlab.rdp.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.WebSecurityConfig;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.BaseTest;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by mjacobson on 13/02/18.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(AdminController.class)
@Import(WebSecurityConfig.class)
public class AdminControllerTest extends BaseTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @MockBean
    private UserService userService;

    @MockBean
    private SiteSettings siteSettings;

    @Before
    public void setUp() {

    }

    @Test
    public void givenNotLoggedIn_whenDeleteUser_thenReturn3xx()
            throws Exception {

        User user = createUser( 1 );
        given( userService.findUserById( Matchers.eq( 1 ) ) ).willReturn( user );

        mvc.perform( get( "/admin/user/1/delete" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().is3xxRedirection() );
    }

    @Test
    @WithMockUser
    public void givenLoggedInAsUser_whenDeleteUser_thenReturn403()
            throws Exception {

        User me = createUser( 1 );

        given( userService.findUserById( Matchers.eq( 1 ) ) ).willReturn( me );

        mvc.perform( get( "/admin/user/1/delete" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    public void givenLoggedInAsManager_whenDeleteUser_thenReturn403()
            throws Exception {

        User me = createUser( 1 );

        given( userService.findUserById( Matchers.eq( 1 ) ) ).willReturn( me );

        mvc.perform( get( "/admin/user/1/delete" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    public void givenLoggedInAsAdmin_whenDeleteUser_thenSucceed()
            throws Exception {

        User me = createUser( 1 );

        given( userService.findUserById( Matchers.eq( 1 ) ) ).willReturn( me );

        mvc.perform( get( "/admin/user/1/delete" )
                .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isOk() );
    }

}
