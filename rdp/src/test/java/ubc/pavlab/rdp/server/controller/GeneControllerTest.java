/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import gemma.gsec.authentication.UserService;

import java.util.Collection;
import java.util.HashSet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.ncbi.NcbiCache;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Unit tests for GeneController methods
 * 
 * @author ptan
 * @version $Id$
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class GeneControllerTest extends BaseSpringContextTest {

    @Autowired
    private GeneController geneController;

    @Autowired
    private NcbiCache cache;

    @Autowired
    private ResearcherService researcherService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private GeneService geneService;

    private final String taxon = "Human";

    private Gene gene;

    private Gene badGene;

    private Researcher researcher;

    private MockMvc mockMvc;

    private Researcher findOrCreateResearcher( String userName ) {
        Researcher researcher = researcherService.findByUserName( userName );
        if ( researcher == null ) {
            User contact = ( User ) userService.findByUserName( userName );
            researcher = new Researcher();
            researcher.setContact( contact );
            researcher = researcherService.create( researcher );

            assertEquals( researcher.getId(), researcherService.findByUserName( userName ).getId() );
        }
        return researcher;
    }

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup( this.wac ).build();

        researcher = findOrCreateResearcher( "administrator" );
        gene = new Gene( "12345", taxon, "BABAM1", "BRISC and BRCA1", "alias-a1,alias-a2" );
        badGene = new Gene();
        badGene.setNcbiGeneId( "12345" );
    }

    @After
    public void tearDown() {
        try {
            researcher.setContact( null ); // Set to null so that deleting the researcher won't delete the user...
            researcherService.delete( researcher );
            // geneService.delete( gene );
            // geneService.delete( badGene );

            Gene savedGene = geneService.findByOfficialSymbol( "BABAM1", taxon );
            geneService.delete( savedGene );

            // don't delete the administrator researcher anymore
            // researcherService.delete( researcher );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     * @param genes
     * @param taxon
     */
    private static void initCache( NcbiCache cache, Collection<Gene> genes, String taxon ) {
        genes.add( new Gene( "1", taxon, "aaa", "gene aa", "alias-a1,alias-a2" ) ); // match symbol exact first
        genes.add( new Gene( "2", taxon, "aaaab", "gene ab", "alias-ab,alias-ab2" ) ); // match symbol partial
                                                                                       // second
        genes.add( new Gene( "3", taxon, "dddd", "aaa gene dd", "alias-dd1,alias-dd2" ) ); // match name third
        genes.add( new Gene( "4", taxon, "ccccc", "gene ccc", "alias-cc1,aaaalias-cc2" ) ); // match alias fourth
        genes.add( new Gene( "5", taxon, "caaaa", "gene ca", "alias-ca1,alias-ca2" ) ); // not symbol suffix
        genes.add( new Gene( "6", taxon, "bbb", "gene bbaaaa", "alias-b1" ) ); // not name suffix
        genes.add( new Gene( "7", "Fish", "aaafish", "gene aa", "alias-a1,alias-a2" ) ); // not taxon

        cache.putAll( genes );
    }

    @Test
    public void testSaveLoadResearcherGenes() throws Exception {

        String genesJsonOk = ( new JSONObject( gene ) ).toString();
        String genesJsonMissingInfo = ( new JSONObject( badGene ) ).toString();

        // this doesn't work, officialSymbol and taxon is required for all genes
        this.mockMvc
                .perform(
                        put( "/saveResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "genes[]", genesJsonMissingInfo ).param( "taxonCommonName", taxon )
                                .param( "taxonDescriptions", "{}" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( false ) );

        // this works, we saved the gene
        this.mockMvc
                .perform(
                        put( "/saveResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "genes[]", genesJsonOk ).param( "taxonCommonName", taxon )
                                .param( "taxonDescriptions", "{}" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) );

        // now let's try loading the saved gene
        this.mockMvc
                .perform(
                        get( "/loadResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON ).param(
                                "taxonCommonName", taxon ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.data[0].officialSymbol" ).value( gene.getOfficialSymbol() ) )
                .andExpect( jsonPath( "$.data[0].taxon" ).value( taxon ) );
    }

    @Test
    public void testFindGenesByGeneSymbols() throws Exception {
        Collection<Gene> genes = new HashSet<>();
        initCache( cache, genes, taxon );
        this.mockMvc
                .perform(
                        get( "/findGenesByGeneSymbols.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "symbols", "Aaa\naaaab\nNOT_FOUND" ).param( "taxon", taxon ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.message" ).value( "1 symbols not found: NOT_FOUND" ) )
                .andExpect( jsonPath( "$.data[0][0].officialSymbol" ).value( "aaaab" ) )
                .andExpect( jsonPath( "$.data[0][1].officialSymbol" ).value( "aaa" ) );
    }

    @Test
    public void testSearchGenes() throws Exception {
        Collection<Gene> genes = new HashSet<>();
        initCache( cache, genes, taxon );
        this.mockMvc
                .perform(
                        get( "/searchGenes.html" ).contentType( MediaType.APPLICATION_JSON ).param( "query", "aAa" )
                                .param( "taxon", taxon ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.data[0].officialSymbol" ).value( "aaa" ) )
                .andExpect( jsonPath( "$.data[1].officialSymbol" ).value( "aaaab" ) )
                .andExpect( jsonPath( "$.data[2].officialSymbol" ).value( "dddd" ) )
                .andExpect( jsonPath( "$.data[3].officialSymbol" ).value( "ccccc" ) );

        this.mockMvc
                .perform(
                        get( "/searchGenes.html" ).contentType( MediaType.APPLICATION_JSON ).param( "query", "aAa" )
                                .param( "taxon", "Fish" ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.data[0].officialSymbol" ).value( "aaafish" ) );
    }
}
