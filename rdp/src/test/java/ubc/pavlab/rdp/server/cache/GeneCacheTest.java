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

package ubc.pavlab.rdp.server.cache;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
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
public class GeneCacheTest extends BaseSpringContextTest {

    @Autowired
    private GeneCache cache;

    private static Long taxonId = 9606L;
    private static Long taxonId2 = 562L;

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     */
    private static void initCache( GeneCache cache ) {
        Collection<Gene> genes = new HashSet<>();
        genes.add( new Gene( 1L, taxonId, "aaa", "gene aa", "alias-a1,alias-a2" ) ); // match symbol exact first
        genes.add( new Gene( 2L, taxonId, "aaaab", "gene ab", "alias-ab,alias-ab2" ) ); // match symbol partial
                                                                                        // second
        genes.add( new Gene( 3L, taxonId, "dddd", "aaa gene dd", "alias-dd1,alias-dd2" ) ); // match name third
        genes.add( new Gene( 4L, taxonId, "ccccc", "gene ccc", "alias-cc1,aaaalias-cc2" ) ); // match alias fourth
        genes.add( new Gene( 5L, taxonId, "caaaa", "gene ca", "alias-ca1,alias-ca2" ) ); // not symbol suffix
        genes.add( new Gene( 6L, taxonId, "bbb", "gene bbaaaa", "alias-b1" ) ); // not name suffix
        genes.add( new Gene( 7L, taxonId2, "aaafish", "gene aa", "alias-a1,alias-a2" ) ); // not taxon

        cache.putAll( genes );
    }

    @Before
    public void setUp() {

        initCache( cache );
    }

    @After
    public void tearDown() {

        cache.clearAll();
    }

    /**
     * See Bug 4187
     */
    @Test
    public void testFetchByQuery() {
        Collection<Gene> results = cache.fetchByQuery( "aaa", taxonId );
        Long[] expectedNcbiGeneIds = new Long[] { 1L, 2L, 3L, 6L, 4L };
        assertEquals( expectedNcbiGeneIds.length, results.size() );
        int i = 0;
        for ( Gene g : results ) {
            assertEquals( expectedNcbiGeneIds[i], g.getId() );
            i++;
        }
    }

    @Test
    public void testFetchBySymbolsAndTaxon() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchBySymbolsAndTaxon( symbols, taxonId );
        assertEquals( 2, results.size() );
    }

    @Test
    public void testFetchBySymbols() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchBySymbols( symbols );
        assertEquals( 3, results.size() );
    }

    @Test
    public void testFetchByTaxons() {
        Collection<Long> taxons = new HashSet<>();
        taxons.add( taxonId );
        Collection<Gene> results = cache.fetchByTaxons( taxons );
        assertEquals( 6, results.size() );

        taxons.add( taxonId2 );
        results = cache.fetchByTaxons( taxons );
        assertEquals( 7, results.size() );

        taxons.remove( taxonId );
        results = cache.fetchByTaxons( taxons );
        assertEquals( 1, results.size() );
    }

    @Test
    public void testFetchById() {
        Collection<Long> ids = new HashSet<>();
        ids.add( 1L );
        ids.add( 3L );
        ids.add( 7L );
        ids.add( 8L );

        Collection<Gene> results = cache.fetchByIds( ids );
        assertEquals( 3, results.size() );
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
