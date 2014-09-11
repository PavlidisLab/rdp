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

package ubc.pavlab.rdp.server.ncbi;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Tests NcbiCacheImpl
 * 
 * @author mjacobson
 * @version $Id$
 */
public class NcbiCacheTest extends BaseSpringContextTest {

    @Autowired
    private NcbiCache cache;

    private Collection<Gene> genes = new ArrayList<>();

    private String taxon = "Human";

    private static Gene createGene( String ncbiGeneId, String taxon, String officialSymbol, String officialName,
            String aliases ) {
        Gene gene = new Gene();
        gene.setNcbiGeneId( ncbiGeneId );
        gene.setTaxon( taxon );
        gene.setOfficialSymbol( officialSymbol );
        gene.setOfficialName( officialName );
        gene.parseAliases( aliases );

        return gene;
    }

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     * @param genes
     * @param taxon
     */
    public static void initCache( NcbiCache cache, Collection<Gene> genes, String taxon ) {
        genes.add( createGene( "1", taxon, "aaa", "gene aa", "alias-a1,alias-a2" ) ); // match symbol exact first
        genes.add( createGene( "2", taxon, "aaaab", "gene ab", "alias-ab,alias-ab2" ) ); // match symbol partial
                                                                                         // second
        genes.add( createGene( "3", taxon, "dddd", "aaa gene dd", "alias-dd1,alias-dd2" ) ); // match name third
        genes.add( createGene( "4", taxon, "ccccc", "gene ccc", "alias-cc1,aaaalias-cc2" ) ); // match alias fourth
        genes.add( createGene( "5", taxon, "caaaa", "gene ca", "alias-ca1,alias-ca2" ) ); // not symbol suffix
        genes.add( createGene( "6", taxon, "bbb", "gene bbaaaa", "alias-b1" ) ); // not name suffix
        genes.add( createGene( "7", "Fish", "aaafish", "gene aa", "alias-a1,alias-a2" ) ); // not taxon

        cache.putAll( genes );
    }

    @Before
    public void setUp() {

        initCache( cache, genes, taxon );
    }

    /**
     * See Bug 4187
     */
    @Test
    public void testFindGenes() {
        Collection<Gene> results = cache.findGenes( "aaa", taxon );
        String[] expectedNcbiGeneIds = new String[] { "1", "2", "3", "4" };
        assertEquals( expectedNcbiGeneIds.length, results.size() );
        int i = 0;
        for ( Gene g : results ) {
            assertEquals( expectedNcbiGeneIds[i], g.getNcbiGeneId() );
            i++;
        }
    }
}
