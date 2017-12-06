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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.service.GeneAnnotationService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneAnnotationServiceTest extends BaseSpringContextTest {

    @Autowired
    GeneAnnotationService geneAnnotationService;

    @Autowired
    GeneService geneService;

    private final static Taxon taxon = new Taxon( 9606L , "Homo sapiens", "human");
    private final static Taxon taxon2 = new Taxon( 562L , "Escherichia coli", "e. coli");
    private List<GeneAnnotation> geneAnnotations = new ArrayList<GeneAnnotation>();
    private List<Gene> genes = new ArrayList<Gene>();

    @Before
    public void setUp() {
        genes.add( new Gene( 1L, taxon, "AAA", "gene aaa", "alias-a1|alias-a2" ) );
        genes.add( new Gene( 2L, taxon, "BBB", "gene bbb", "alias-b1|alias-b2" ) );
        genes.add( new Gene( 3L, taxon2, "CCC", "gene ccc", "alias-c1|alias-c2" ) );
        for ( Gene gene : genes ) {
            geneService.create( gene );
        }
        // GeneId, GO_ID, tax_id, GO_term, Category
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000001", "Process 1", "Process" ) ); // 0-2
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000002", "Process 2", "Process" ) ); // 1-1
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000003", "Process 3", "Process" ) ); // 2-1
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000004", "Process 4", "Process" ) ); // 3-2
        geneAnnotations.add( new GeneAnnotation( genes.get( 1 ), "GO:0000005", "Process 5", "Process" ) ); // 4-2
        geneAnnotations.add( new GeneAnnotation( genes.get( 1 ), "GO:0000006", "Process 6", "Process" ) ); // 5-2
        geneAnnotations.add( new GeneAnnotation( genes.get( 1 ), "GO:0000007", "Process 7", "Process" ) ); // 6-1
        geneAnnotations.add( new GeneAnnotation( genes.get( 2 ), "GO:0000008", "Process 8", "Process" ) ); // 7-2
        geneAnnotations.add( new GeneAnnotation( genes.get( 2 ), "GO:0000009", "Process 9", "Process" ) ); // 8-1
        geneAnnotations.add( new GeneAnnotation( genes.get( 2 ), "GO:0000010", "Process 10", "Process" ) ); // 9-3

        // Crossovers
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000005", "Process 5", "Process" ) ); // 10
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000008", "Process 8", "Process" ) ); // 11
        geneAnnotations.add( new GeneAnnotation( genes.get( 0 ), "GO:0000010", "Process 10", "Process" ) ); // 12

        geneAnnotations.add( new GeneAnnotation( genes.get( 1 ), "GO:0000001", "Process 1", "Process" ) ); // 13
        geneAnnotations.add( new GeneAnnotation( genes.get( 1 ), "GO:0000010", "Process 10", "Process" ) ); // 14

        geneAnnotations.add( new GeneAnnotation( genes.get( 2 ), "GO:0000006", "Process 6", "Process" ) ); // 15
        geneAnnotations.add( new GeneAnnotation( genes.get( 2 ), "GO:0000004", "Process 4", "Process" ) ); // 16

        for ( GeneAnnotation geneAnnotation : geneAnnotations ) {
            geneAnnotationService.create( geneAnnotation );
        }

    }

    @After
    public void tearDown() {

        for ( GeneAnnotation geneAnnotation : geneAnnotations ) {
            geneAnnotationService.delete( geneAnnotation );
        }

        for ( Gene gene : genes ) {
            geneService.delete( gene );
        }

        geneAnnotations.clear();
        genes.clear();
    }

    @Test
    public void testFindByGeneOntologyId() {
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGeneOntologyId( "GO:0000001" );
        assertEquals( 2, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 0 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 13 ) ) );

        ga = geneAnnotationService.findByGeneOntologyId( "GO:0000002" );
        assertEquals( 1, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 1 ) ) );

        ga = geneAnnotationService.findByGeneOntologyId( "GO:0000010" );
        assertEquals( 3, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 9 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 12 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 14 ) ) );
    }

    @Test
    public void testFindByGeneOntologyIdAndTaxon() {
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( "GO:0000010", taxon.getId() );
        assertEquals( 2, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 12 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 14 ) ) );

        ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( "GO:0000010", taxon2.getId() );
        assertEquals( 1, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 9 ) ) );

        ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( "GO:0000006", taxon.getId() );
        assertEquals( 1, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 5 ) ) );
    }

    @Test
    public void testFindByGene() {
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGene( genes.get( 0 ) );
        assertEquals( 7, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 0 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 1 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 2 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 3 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 10 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 11 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 12 ) ) );

        ga = geneAnnotationService.findByGene( genes.get( 1 ) );
        assertEquals( 5, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 4 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 5 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 6 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 13 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 14 ) ) );

        ga = geneAnnotationService.findByGene( genes.get( 2 ) );
        assertEquals( 5, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 7 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 8 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 9 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 15 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 16 ) ) );

    }

    @Test
    public void testFindByGeneAndGeneOntologyId() {
        GeneAnnotation ga = geneAnnotationService.findByGeneAndGeneOntologyId( genes.get( 0 ), "GO:0000001" );
        assertEquals( geneAnnotations.get( 0 ), ga );

        ga = geneAnnotationService.findByGeneAndGeneOntologyId( genes.get( 0 ), "GO:0000006" );
        assertNull( ga );

        ga = geneAnnotationService.findByGeneAndGeneOntologyId( genes.get( 2 ), "GO:0000010" );
        assertEquals( geneAnnotations.get( 9 ), ga );

        ga = geneAnnotationService.findByGeneAndGeneOntologyId( genes.get( 1 ), "GO:0000005" );
        assertEquals( geneAnnotations.get( 4 ), ga );
    }

    @Test
    public void testCountGenesForGeneOntologyId() {
        long cnt = geneAnnotationService.countGenesForGeneOntologyId( "GO:0000001" );
        assertEquals( 2, cnt );

        cnt = geneAnnotationService.countGenesForGeneOntologyId( "GO:0000002" );
        assertEquals( 1, cnt );

        cnt = geneAnnotationService.countGenesForGeneOntologyId( "GO:0000010" );
        assertEquals( 3, cnt );

    }

    @Test
    public void testFindByGeneLimitedByTermSize() {
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGeneLimitedByTermSize( genes.get( 0 ), 3 );
        assertEquals( 7, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 0 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 1 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 2 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 3 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 10 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 11 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 12 ) ) );

        ga = geneAnnotationService.findByGeneLimitedByTermSize( genes.get( 0 ), 2 );
        assertEquals( 6, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 0 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 1 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 2 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 3 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 10 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 11 ) ) );

        ga = geneAnnotationService.findByGeneLimitedByTermSize( genes.get( 0 ), 1 );
        assertEquals( 2, ga.size() );
        assertTrue( ga.contains( geneAnnotations.get( 1 ) ) );
        assertTrue( ga.contains( geneAnnotations.get( 2 ) ) );

    }

    @Test
    public void testFindRelatedTerms() {
        Map<GeneOntologyTerm, Long> goTermsMap = geneAnnotationService.findRelatedTerms( genes, 2 );
        Map<GeneOntologyTerm, Long> goTermsMapCorrect = new HashMap<GeneOntologyTerm, Long>();
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000001", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000004", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000005", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000006", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000008", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000010", "" ), 3L );

        assertEquals( goTermsMapCorrect, goTermsMap );

        goTermsMap = geneAnnotationService.findRelatedTerms( genes, 3 );
        assertEquals( 1, goTermsMap.size() );
        assertTrue( goTermsMap.containsKey( new GeneOntologyTerm( "GO:0000010", "" ) ) );

    }

    @Test
    public void testFindRelatedTermsLimitedByTermSize() {
        Map<GeneOntologyTerm, Long> goTermsMap = geneAnnotationService.findRelatedTermsLimitedByTermSize( genes, 2, 3 );
        Map<GeneOntologyTerm, Long> goTermsMapCorrect = new HashMap<GeneOntologyTerm, Long>();
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000001", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000004", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000005", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000006", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000008", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000010", "" ), 3L );

        assertEquals( goTermsMapCorrect, goTermsMap );

        goTermsMap = geneAnnotationService.findRelatedTermsLimitedByTermSize( genes, 2, 2 );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000010", "" ) );
        assertEquals( goTermsMapCorrect, goTermsMap );

    }

    @Test
    public void testTruncateGeneAnnotationTable() {

        assertTrue( geneAnnotationService.loadAll().size() > 0 );

        geneAnnotationService.truncateGeneAnnotationTable();

        assertEquals( 0, geneAnnotationService.loadAll().size() );

    }

    @Test
    public void testAnnotationToGeneOntologyId() {
        Collection<GeneOntologyTerm> goTerms = geneAnnotationService.annotationToGeneOntologyId( geneAnnotations );

        assertEquals( 10, goTerms.size() );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000001", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000002", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000003", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000004", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000005", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000006", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000007", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000008", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000009", "" ) ) );
        assertTrue( goTerms.contains( new GeneOntologyTerm( "GO:0000010", "" ) ) );

    }

    @Test
    public void testFindTermsAndFrequenciesByGenes() {
        Map<GeneOntologyTerm, Long> goTermsMap = geneAnnotationService.findTermsAndFrequenciesByGenes( genes );
        Map<GeneOntologyTerm, Long> goTermsMapCorrect = new HashMap<GeneOntologyTerm, Long>();
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000001", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000002", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000003", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000004", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000005", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000006", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000007", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000008", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000009", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000010", "" ), 3L );

        assertEquals( goTermsMapCorrect, goTermsMap );

    }

    @Test
    public void testFindTermsAndFrequenciesByGenesLimitedByTermSize() {
        Map<GeneOntologyTerm, Long> goTermsMap = geneAnnotationService.findTermsAndFrequenciesByGenesLimitedByTermSize(
                genes, 3 );
        Map<GeneOntologyTerm, Long> goTermsMapCorrect = new HashMap<GeneOntologyTerm, Long>();
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000001", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000002", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000003", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000004", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000005", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000006", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000007", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000008", "" ), 2L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000009", "" ), 1L );
        goTermsMapCorrect.put( new GeneOntologyTerm( "GO:0000010", "" ), 3L );

        assertEquals( goTermsMapCorrect, goTermsMap );

        goTermsMap = geneAnnotationService.findTermsAndFrequenciesByGenesLimitedByTermSize( genes, 2 );

        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000010", "" ) );
        assertEquals( goTermsMapCorrect, goTermsMap );

        goTermsMap = geneAnnotationService.findTermsAndFrequenciesByGenesLimitedByTermSize( genes, 1 );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000001", "" ) );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000004", "" ) );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000005", "" ) );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000006", "" ) );
        goTermsMapCorrect.remove( new GeneOntologyTerm( "GO:0000008", "" ) );
        assertEquals( goTermsMapCorrect, goTermsMap );

    }

    @Test
    public void testFindTermsByGenes() {
        Collection<GeneOntologyTerm> goTerms = geneAnnotationService.findTermsByGenes( genes );
        Collection<GeneOntologyTerm> goTermsCorrect = new HashSet<GeneOntologyTerm>();
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000001", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000002", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000003", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000004", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000005", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000006", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000007", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000008", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000009", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000010", "" ) );

        assertEquals( goTermsCorrect, goTerms );

        Collection<Gene> genes2 = new HashSet<Gene>();
        genes2.add( genes.get( 0 ) );
        genes2.add( genes.get( 1 ) );

        goTerms = geneAnnotationService.findTermsByGenes( genes2 );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000009", "" ) );
        assertEquals( goTermsCorrect, goTerms );

        genes2.remove( genes.get( 1 ) );

        goTerms = geneAnnotationService.findTermsByGenes( genes2 );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000006", "" ) );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000007", "" ) );
        assertEquals( goTermsCorrect, goTerms );

    }

    @Test
    public void testFindTermsByGenesLimitedByTermSize() {
        Collection<GeneOntologyTerm> goTerms = geneAnnotationService.findTermsByGenesLimitedByTermSize( genes, 3 );
        Collection<GeneOntologyTerm> goTermsCorrect = new HashSet<GeneOntologyTerm>();
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000001", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000002", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000003", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000004", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000005", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000006", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000007", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000008", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000009", "" ) );
        goTermsCorrect.add( new GeneOntologyTerm( "GO:0000010", "" ) );
        assertEquals( goTermsCorrect, goTerms );

        goTerms = geneAnnotationService.findTermsByGenesLimitedByTermSize( genes, 2 );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000010", "" ) );
        assertEquals( goTermsCorrect, goTerms );

        goTerms = geneAnnotationService.findTermsByGenesLimitedByTermSize( genes, 1 );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000001", "" ) );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000004", "" ) );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000005", "" ) );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000006", "" ) );
        goTermsCorrect.remove( new GeneOntologyTerm( "GO:0000008", "" ) );
        assertEquals( goTermsCorrect, goTerms );
    }

}
