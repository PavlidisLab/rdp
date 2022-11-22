package ubc.pavlab.rdp.controllers;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;
import static ubc.pavlab.rdp.util.TestUtils.createTerm;

@WebMvcTest(TermController.class)
@TestPropertySource("classpath:application.properties")
@Import({ ApplicationSettings.class, SiteSettings.class })
public class TermControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    private TaxonService taxonService;

    @MockBean
    private GOService goService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Test
    public void searchTermsByQueryAndTaxon_thenReturnMatchingTerms() throws Exception {
        Taxon taxon = createTaxon( 1 );
        when( taxonService.findById( 1 ) ).thenReturn( taxon );
        GeneOntologyTermInfo term = createTerm( "GO:0000001" );
        when( goService.search( "GO:0000001", taxon, 10 ) )
                .thenReturn( Collections.singletonList( new SearchResult<>( TermMatchType.EXACT_ID, 0, term.getGoId(), term.getName(), term ) ) );
        mvc.perform( get( "/taxon/1/term/search" )
                        .param( "query", "GO:0000001" )
                        .param( "max", "10" ) )
                .andExpect( jsonPath( "$[0].matchType" ).value( "Exact Id" ) )
                .andExpect( jsonPath( "$[0].id" ).value( 0 ) )
                .andExpect( jsonPath( "$[0].label" ).value( term.getGoId() ) )
                .andExpect( jsonPath( "$[0].description" ).value( term.getName() ) )
                .andExpect( jsonPath( "$[0].extras" ).value( (String) null ) );
    }
}
