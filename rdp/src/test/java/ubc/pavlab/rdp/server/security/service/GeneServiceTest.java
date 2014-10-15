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

package ubc.pavlab.rdp.server.security.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
public class GeneServiceTest extends BaseSpringContextTest {

    @Autowired
    GeneDao geneDao;

    @Autowired
    GeneService geneService;

    private Gene gene;
    private Gene gene2;
    private Gene gene3;
    private String officialSymbol = "aaa";
    private Long taxonId = 9606L;

    private String officialSymbol2 = "aaafish";
    private Long taxonId2 = 562L;

    @Before
    public void setUp() {
        gene = new Gene( 1L, taxonId, officialSymbol, "gene aa", "alias-a1,alias-a2" );
        gene2 = new Gene( 2L, taxonId2, officialSymbol2, "gene aa", "alias-a1,alias-a2" );
        gene3 = new Gene( 3L, taxonId2, officialSymbol, "gene aa", "alias-a1,alias-a2" );
    }

    @Test
    public void testFindByOfficialSymbolAndTaxon() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        assertEquals( savedGene.getOfficialSymbol(), geneService.findByOfficialSymbolAndTaxon( officialSymbol, taxonId )
                .getOfficialSymbol() );
        assertEquals( savedGene2.getOfficialSymbol(),
                geneService.findByOfficialSymbolAndTaxon( officialSymbol2, taxonId2 ).getOfficialSymbol() );
        assertEquals( savedGene3.getOfficialSymbol(),
                geneService.findByOfficialSymbolAndTaxon( officialSymbol, taxonId2 ).getOfficialSymbol() );

        assertEquals( 2, geneService.findByOfficialSymbol( officialSymbol ).size() );
        assertEquals( 1, geneService.findByOfficialSymbol( officialSymbol2 ).size() );

        geneService.delete( savedGene );
        assertNull( geneService.findByOfficialSymbolAndTaxon( officialSymbol, taxonId ) );
        geneService.delete( savedGene2 );
        assertNull( geneService.findByOfficialSymbolAndTaxon( officialSymbol2, taxonId2 ) );
        geneService.delete( savedGene3 );
        assertNull( geneService.findByOfficialSymbolAndTaxon( officialSymbol, taxonId2 ) );

    }

    @Test
    public void testFindByOfficialSymbol() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        Collection<Gene> res = geneService.findByOfficialSymbol( officialSymbol );
        assertTrue( res.contains( savedGene ) );
        assertTrue( res.contains( savedGene3 ) );

        res = geneService.findByOfficialSymbol( officialSymbol2 );
        assertTrue( res.contains( savedGene2 ) );

        geneService.delete( savedGene );
        res = geneService.findByOfficialSymbol( officialSymbol );
        assertTrue( res.contains( savedGene3 ) );

        geneService.delete( savedGene2 );
        res = geneService.findByOfficialSymbol( officialSymbol2 );
        assertNull( res );

        geneService.delete( savedGene3 );
        res = geneService.findByOfficialSymbol( officialSymbol );
        assertNull( res );

    }

    @Test
    public void testFindById() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        assertEquals( savedGene.getId(), geneService.findById( savedGene.getId() ).getId() );
        assertEquals( savedGene2.getId(), geneService.findById( savedGene2.getId() ).getId() );
        assertEquals( savedGene3.getId(), geneService.findById( savedGene3.getId() ).getId() );

        geneService.delete( savedGene );
        assertNull( geneService.findById( savedGene.getId() ) );
        geneService.delete( savedGene2 );
        assertNull( geneService.findById( savedGene2.getId() ) );
        geneService.delete( savedGene3 );
        assertNull( geneService.findById( savedGene3.getId() ) );

    }

    @Test
    public void testFindByTaxonId() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        Collection<Gene> res = geneService.findByTaxonId( taxonId );
        assertTrue( res.contains( savedGene ) );

        res = geneService.findByTaxonId( taxonId2 );
        assertTrue( res.contains( savedGene2 ) );
        assertTrue( res.contains( savedGene3 ) );

        geneService.delete( savedGene );
        res = geneService.findByTaxonId( taxonId );
        assertNull( res );

        geneService.delete( savedGene2 );
        res = geneService.findByTaxonId( taxonId2 );
        assertTrue( res.contains( savedGene3 ) );

        geneService.delete( savedGene3 );
        res = geneService.findByTaxonId( taxonId );
        assertNull( res );

    }

    @Test
    public void testDeserializeGenes() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );
        Gene badGene = new Gene(); // Missing id
        badGene.setOfficialSymbol( "officialSymbol" );
        badGene.setOfficialName( "officialName" );

        String[] genesJsonOk = new String[] { ( new JSONObject( gene ) ).toString(),
                ( new JSONObject( gene2 ) ).toString() };
        String[] genesJsonMissingInfo = new String[] { ( new JSONObject( badGene ) ).toString() };

        HashMap<Gene, TierType> results = geneService.deserializeGenes( genesJsonOk );
        assertEquals( 2, results.size() );

        geneService.delete( savedGene );
        geneService.delete( savedGene2 );
        geneService.delete( savedGene3 );

        // Genes no longer in db
        results = geneService.deserializeGenes( genesJsonOk );
        assertEquals( 0, results.size() );

        try {
            results = geneService.deserializeGenes( genesJsonMissingInfo );
            fail( "Method should throw IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }

    }

    @Test
    public void testTruncateGeneTable() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        assertTrue( geneService.loadAll().size() > 0 );

        geneService.truncateGeneTable();

        assertEquals( 0, geneService.loadAll().size() );

    }

}
