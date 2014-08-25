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
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.model.Gene;
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
    private String officialSymbol = "GENEA";

    @Before
    public void setUp() {
        gene = new Gene( officialSymbol );
        gene = geneService.create( gene );
        assertNotNull( gene.getId() );
        assertEquals( officialSymbol, gene.getOfficialSymbol() );
    }

    @After
    public void tearDown() {
        geneService.delete( gene );
        assertEquals( 0, geneService.findByOfficalSymbol( officialSymbol ).size() );
    }

    @Test
    public void testFindBySymbol() {
        assertEquals( gene.getOfficialSymbol(), geneService.findByOfficalSymbol( officialSymbol ).iterator().next()
                .getOfficialSymbol() );
        assertEquals( 0, geneService.findByOfficalSymbol( "GENE_DOES_NOT_EXIST" ).size() );
    }
}
