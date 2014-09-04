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

import ubc.pavlab.rdp.server.biomartquery.BioMartCache;
import ubc.pavlab.rdp.server.biomartquery.BioMartCacheTest;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
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
    private BioMartCache cache;

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

    private Researcher researcher;

    private MockMvc mockMvc;

    private void createResearcher( String userName ) {
        researcher = researcherService.findByUserName( userName );
        if ( researcher == null ) {
            User contact = ( User ) userService.findByUserName( userName );
            researcher = new Researcher();
            researcher.setContact( contact );
            researcher = researcherService.create( researcher );

            assertEquals( researcher.getId(), researcherService.findByUserName( userName ).getId() );
        }
    }

    private void createGene() {
        gene = new Gene();
        gene.setEnsemblId( "ENSG00000105393" );
        gene.setOfficialSymbol( "BABAM1" );
        gene.setOfficialName( "BRISC and BRCA1" );
        gene.setNcbiGeneId( "12345" );
        gene.setTaxon( taxon );
    }

    @Before
    public void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup( this.wac ).build();

        createResearcher( "administrator" );
        createGene();
    }

    @After
    public void tearDown() {
        try {
            geneService.delete( gene );

            // don't delete the administrator researcher anymore
            // researcherService.delete( researcher );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSaveLoadResearcherGenes() throws Exception {

        String genesJsonMissingKey = ( new JSONObject( gene ) ).toString();

        // String genesJsonMissingKey =
        // "{'ensemblId':'ENSG00000105393','officialSymbol':'BABAM1','officialName':'BRISC and BRCA1 A complex member 1','label':'BABAM1','geneBioType':'protein_coding','key':'BABAM1:human','taxon':'human','genomicRange':{'baseStart':17378159,'baseEnd':17392058,'label':'19:17378159-17392058','htmlLabel':'19:17378159-17392058','bin':65910,'chromosome':'19','tooltip':'19:17378159-17392058'},'text':'<b>BABAM1</b> BRISC and BRCA1 A complex member 1'}";
        String genesJsonOk = "{ \"BABAM1:Human\" : " + genesJsonMissingKey + " }";

        // this doesn't work, a gene key is required
        this.mockMvc
                .perform(
                        put( "/saveResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "genes", genesJsonMissingKey ).param( "taxonCommonName", taxon ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.success" ).value( false ) );

        // this works, we saved the gene
        this.mockMvc
                .perform(
                        put( "/saveResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "genes", genesJsonOk ).param( "taxonCommonName", taxon ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.success" ).value( true ) );

        // now let's try loading the saved gene
        this.mockMvc
                .perform(
                        get( "/loadResearcherGenes.html" ).contentType( MediaType.APPLICATION_JSON ).param(
                                "taxonCommonName", taxon ) ).andExpect( status().isOk() )
                .andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.data[0].officialSymbol" ).value( gene.getOfficialSymbol() ) );
    }

    @Test
    public void testFindGenesByGeneSymbols() throws Exception {
        Collection<Gene> genes = new HashSet<>();
        BioMartCacheTest.initCache( cache, genes, taxon );
        this.mockMvc
                .perform(
                        get( "/findGenesByGeneSymbols.html" ).contentType( MediaType.APPLICATION_JSON )
                                .param( "symbols", "Aaa,aaaab,NOT_FOUND" ).param( "taxon", taxon ) )
                .andExpect( status().isOk() ).andExpect( jsonPath( "$.success" ).value( true ) )
                .andExpect( jsonPath( "$.message" ).value( "1 symbols not found: NOT_FOUND" ) )
                .andExpect( jsonPath( "$.data[0][0].officialSymbol" ).value( "aaaab" ) )
                .andExpect( jsonPath( "$.data[0][1].officialSymbol" ).value( "aaa" ) );
    }
}
