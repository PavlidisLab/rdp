/*
 * The rdp project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubc.pavlab.rdp.server.biomartquery;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAlias;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Tests BioMartCacheImpl
 * 
 * @author ptan
 * @version $Id: $
 */
public class BioMartCacheTest extends BaseSpringContextTest {

    @Autowired
    private BioMartCache cache;

    private Collection<Gene> genes = new ArrayList<>();

    private String taxon = "human";

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     * @param genes
     * @param taxon
     */
    public static void initCache( BioMartCache cache, Collection<Gene> genes, String taxon ) {
        HashMap<Gene, String> geneMap = new HashMap<>();
        geneMap.put( new Gene( "ENSG01", taxon, "aaa", "gene aa" ), "alias-a1,alias-a2" ); // match symbol exact first
        geneMap.put( new Gene( "ENSG02", taxon, "aaaab", "gene ab" ), "alias-ab,alias-ab2" ); // match symbol partial
                                                                                              // second
        geneMap.put( new Gene( "ENSG03", taxon, "dddd", "aaa gene dd" ), "alias-dd1,alias-dd2" ); // match name third
        geneMap.put( new Gene( "ENSG04", taxon, "ccccc", "gene ccc" ), "alias-cc1,aaaalias-cc2" ); // match alias fourth
        geneMap.put( new Gene( "ENSG05", taxon, "caaaa", "gene ca" ), "alias-ca1,alias-ca2" ); // not symbol suffix
        geneMap.put( new Gene( "ENSG06", taxon, "bbb", "gene bbaaaa" ), "alias-b1" ); // not name suffix
        geneMap.put( new Gene( "ENSG07", "fish", "aaafish", "gene aa" ), "alias-a1,alias-a2" ); // not taxon

        for ( Gene gene : geneMap.keySet() ) {

            String[] parts = geneMap.get( gene ).split( "," );

            Set<GeneAlias> aliases = new HashSet<>();
            for ( int i = 0; i < parts.length; i++ ) {
                aliases.add( new GeneAlias( parts[i] ) );
            }
            gene.setAliases( aliases );
            genes.add( gene );
        }
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
        String[] expectedEnsemblIds = new String[] { "ENSG01", "ENSG02", "ENSG03", "ENSG04" };
        assertEquals( expectedEnsemblIds.length, results.size() );
        int i = 0;
        for ( Gene g : results ) {
            assertEquals( expectedEnsemblIds[i], g.getEnsemblId() );
            i++;
        }
    }
}
