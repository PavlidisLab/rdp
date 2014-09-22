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
import java.util.HashSet;

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

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     * @param genes
     * @param taxon
     */
    public static void initCache( NcbiCache cache, Collection<Gene> genes, String taxon ) {
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

    @Test
    public void testFetchGenesByGeneSymbolsAndTaxon() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchGenesByGeneSymbolsAndTaxon( symbols, taxon );
        assertEquals( 2, results.size() );
    }

    @Test
    public void testFetchGenesByGeneSymbols() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchGenesByGeneSymbols( symbols );
        assertEquals( 3, results.size() );
    }

    @Test
    public void testFetchGenesByGeneTaxon() {
        Collection<String> taxons = new HashSet<>();
        taxons.add( taxon );
        Collection<Gene> results = cache.fetchGenesByGeneTaxon( taxons );
        assertEquals( 6, results.size() );

        taxons.add( "Fish" );
        results = cache.fetchGenesByGeneTaxon( taxons );
        assertEquals( 7, results.size() );

        taxons.remove( taxon );
        results = cache.fetchGenesByGeneTaxon( taxons );
        assertEquals( 1, results.size() );
    }

    @Test
    public void testSize() {
        assertEquals( 7, cache.size() );
    }

    @Test
    public void testClearAll() {
        // This is dependent on testSize()
        assertEquals( 7, cache.size() );
        cache.clearAll();
        assertEquals( 0, cache.size() );

    }

}
