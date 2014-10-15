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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.service.TaxonService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class TaxonServiceTest extends BaseSpringContextTest {

    @Autowired
    TaxonService taxonService;

    private Long taxonId = 9606L;
    private String commonName = "human";

    @Test
    public void testFindById() {
        Taxon t = taxonService.findById( taxonId );
        assertEquals( taxonId, t.getId() );
        assertEquals( commonName, t.getCommonName() );
    }

    @Test
    public void testFindByCommonName() {
        Taxon t = taxonService.findByCommonName( commonName );
        assertEquals( taxonId, t.getId() );
        assertEquals( commonName, t.getCommonName() );
    }
}
