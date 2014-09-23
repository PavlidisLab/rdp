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
import static org.junit.Assert.fail;

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
    private String taxon = "human";

    private String officialSymbol2 = "aaafish";
    private String taxon2 = "Fish";

    @Before
    public void setUp() {
        gene = new Gene( "1", taxon, officialSymbol, "gene aa", "alias-a1,alias-a2" );
        gene2 = new Gene( "2", taxon2, officialSymbol2, "gene aa", "alias-a1,alias-a2" );
        gene3 = new Gene( "3", taxon2, officialSymbol, "gene aa", "alias-a1,alias-a2" );
    }

    @Test
    public void testFindByOfficialSymbol() {
        Gene savedGene = geneService.create( gene );
        Gene savedGene2 = geneService.create( gene2 );
        Gene savedGene3 = geneService.create( gene3 );

        assertEquals( savedGene.getOfficialSymbol(), geneService.findByOfficialSymbol( officialSymbol, taxon )
                .getOfficialSymbol() );
        assertEquals( savedGene2.getOfficialSymbol(), geneService.findByOfficialSymbol( officialSymbol2, taxon2 )
                .getOfficialSymbol() );
        assertEquals( savedGene3.getOfficialSymbol(), geneService.findByOfficialSymbol( officialSymbol, taxon2 )
                .getOfficialSymbol() );

        assertEquals( 2, geneService.findByOfficialSymbol( officialSymbol ).size() );
        assertEquals( 1, geneService.findByOfficialSymbol( officialSymbol2 ).size() );

        geneService.delete( savedGene );
        assertNull( geneService.findByOfficialSymbol( officialSymbol, taxon ) );
        geneService.delete( savedGene2 );
        assertNull( geneService.findByOfficialSymbol( officialSymbol2, taxon2 ) );
        geneService.delete( savedGene3 );
        assertNull( geneService.findByOfficialSymbol( officialSymbol, taxon2 ) );

    }

    @Test
    public void testDeserializeGenes() {
        Gene badGene = new Gene();
        badGene.setNcbiGeneId( "4" );

        String[] genesJsonOk = new String[] { ( new JSONObject( gene ) ).toString(),
                ( new JSONObject( gene2 ) ).toString() };
        String[] genesJsonMissingInfo = new String[] { ( new JSONObject( badGene ) ).toString() };

        HashMap<Gene, TierType> results = geneService.deserializeGenes( genesJsonOk );
        assertEquals( 2, results.size() );

        for ( Gene g : results.keySet() ) {
            geneService.delete( g );
        }

        try {
            results = geneService.deserializeGenes( genesJsonMissingInfo );
            fail( "Method should throw IllegalArgumentException" );
        } catch ( IllegalArgumentException e ) {
        }

    }

}
