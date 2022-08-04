package ubc.pavlab.rdp.controllers;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.services.UserGeneService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.OntologyMessageSource;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
        "rdp.site.host=localhost",
        "rdp.site.contact-email=contact@localhost",
        "rdp.site.admin-email=admin@localhost",
        "rdp.site.mainsite=https://example.com" })
@RunWith(SpringRunner.class)
@WebMvcTest(StatsController.class)
@Import(SiteSettings.class)
public class StatsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ApplicationSettings applicationSettings;

    @MockBean
    private UserService userService;

    @MockBean
    private UserGeneService userGeneService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean
    private OntologyMessageSource ontologyMessageSource;

    @MockBean
    private BuildProperties buildProperties;

    @Test
    public void getStats() throws Exception {
        when( buildProperties.getVersion() ).thenReturn( "1.5.0" );
        mvc.perform( get( "/stats" ) )
                .andExpect( jsonPath( "$.version" ).value( "1.5.0" ) );
    }

    @Test
    public void getStats_thenOnlyReturnAllowedOrigin() throws Exception {
        mvc.perform( get( "/stats" )
                        .header( HttpHeaders.ORIGIN, "https://example.com" ) )
                .andExpect( header().doesNotExist( HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS ) )
                .andExpect( header().string( HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com" ) );
    }

    @Test
    public void getStats_whenRequestIsPreflight_thenReturnAllowedMethodsAndOrigin() throws Exception {
        mvc.perform( options( "/stats" )
                        .header( HttpHeaders.ORIGIN, "https://example.com" )
                        .header( HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET" ) )
                .andExpect( header().string( HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET" ) )
                .andExpect( header().string( HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://example.com" ) );
    }

    @Test
    public void getStats_whenOriginIsNotAllowed_thenReturnForbidden() throws Exception {
        mvc.perform( get( "/stats" )
                        .header( HttpHeaders.ORIGIN, "https://example2.com" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    public void getStats_whenNoOriginIsProvided_thenReturnResult() throws Exception {
        mvc.perform( get( "/stats" ) )
                .andExpect( status().isOk() );
    }

    @Test
    public void getStatsHtml() throws Exception {
        mvc.perform( get( "/stats.html" ) )
                .andExpect( status().is3xxRedirection() )
                .andExpect( redirectedUrl( "/stats" ) );
    }
}