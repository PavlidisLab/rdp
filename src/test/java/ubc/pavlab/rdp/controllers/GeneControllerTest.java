package ubc.pavlab.rdp.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.services.GOService;
import ubc.pavlab.rdp.services.GeneInfoService;
import ubc.pavlab.rdp.services.TaxonService;
import ubc.pavlab.rdp.services.UserService;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.settings.SiteSettings;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static ubc.pavlab.rdp.util.TestUtils.createGene;
import static ubc.pavlab.rdp.util.TestUtils.createTaxon;

@WebMvcTest(GeneController.class)
@TestPropertySource("classpath:application.properties")
@Import({ ApplicationSettings.class, SiteSettings.class })
public class GeneControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private GeneInfoService geneService;

    @MockBean
    private GOService goService;

    @MockBean
    private TaxonService taxonService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private PermissionEvaluator permissionEvaluator;

    @MockBean(name = "ontologyMessageSource")
    private MessageSource ontologyMessageSource;

    @Test
    public void searchGenesByTaxonAndSymbols_thenReturnMatchingGenes() throws Exception {
        Taxon taxon = createTaxon( 1 );
        GeneInfo gene = createGene( 1, taxon );
        when( taxonService.findById( 1 ) ).thenReturn( taxon );
        when( geneService.findBySymbolAndTaxon( "BRCA1", taxon ) ).thenReturn( gene );
        mvc.perform( get( "/taxon/{taxonId}/gene/search", 1 )
                        .param( "symbols", "BRCA1" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.BRCA1" ).value( gene ) );
        verify( geneService ).findBySymbolAndTaxon( "BRCA1", taxon );
    }

    @Test
    public void searchGenesByTaxonAndTerm_thenAutocompleteGenes() throws Exception {
        Taxon taxon = createTaxon( 1 );
        GeneInfo gene = createGene( 1, taxon );
        when( taxonService.findById( 1 ) ).thenReturn( taxon );
        when( geneService.autocomplete( "BRCA1", taxon, 10 ) ).thenReturn( Collections.singletonList( new SearchResult<>( GeneMatchType.EXACT_SYMBOL, gene.getGeneId(), gene.getSymbol(), gene.getName(), gene ) ) );
        mvc.perform( get( "/taxon/{taxonId}/gene/search", 1 )
                        .param( "query", "BRCA1" )
                        .param( "max", "10" ) )
                .andExpect( status().isOk() )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$[0].matchType" ).value( "Exact Symbol" ) )
                .andExpect( jsonPath( "$[0].id" ).value( gene.getGeneId() ) )
                .andExpect( jsonPath( "$[0].label" ).value( gene.getSymbol() ) )
                .andExpect( jsonPath( "$[0].description" ).value( gene.getName() ) )
                .andExpect( jsonPath( "$[0].extras" ).value( (String) null ) );
        verify( geneService ).autocomplete( "BRCA1", taxon, 10 );
    }
}
