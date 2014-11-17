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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.server.security.authentication.UserService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.server.service.ResearcherService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
public class ResearcherServiceTest extends BaseSpringContextTest {

    @Autowired
    ResearcherService researcherService;

    @Autowired
    ResearcherDao researcherDao;

    @Autowired
    UserService userService;

    @Autowired
    UserManager userManager;

    @Autowired
    GeneService geneService;

    private Researcher researcher;
    private Gene gene1;
    private Gene gene2;

    private String email = "foobar@email.com";
    private String username = "foobar";
    private String department = "dept";

    private Long taxonId = 9606L;
    private Long taxonId2 = 562L;

    @Before
    public void setUp() {
        researcher = createResearcher( username, email, department );
        gene1 = new Gene( 1L, taxonId, "aaa", "gene aa", "alias-a1,alias-a2" );
        gene2 = new Gene( 7L, taxonId2, "aaafish", "gene aa", "alias-a1,alias-a2" );
        geneService.create( gene1 );
        geneService.create( gene2 );

    }

    @After
    public void tearDown() {
        researcherService.delete( researcher );
        geneService.delete( gene1 );
        geneService.delete( gene2 );
        assertNull( researcherService.findByEmail( email ) );
        assertNull( userService.findByEmail( email ) );
    }

    private User createUser( String username, String email ) {
        User testContact = new User();
        testContact.setUserName( username );
        testContact.setEmail( email );
        testContact.setEnabled( true );
        userManager.createUser( new UserDetailsImpl( testContact ) );
        testContact = ( User ) userService.findByEmail( email );
        return testContact;
    }

    private Researcher createResearcher( String username, String email, String department ) {
        User testContact = createUser( username, email );

        Researcher testResearcher = new Researcher();
        testResearcher = new Researcher();
        testResearcher.setContact( testContact );
        testResearcher.setDepartment( department );
        return researcherService.create( testResearcher );
    }

    @Test
    public void testFindByEmail() throws Exception {

        super.runAsAdmin();

        try {
            assertNull( researcherService.findByEmail( "noEmail" ) );
            assertNotNull( researcher );
            Researcher r = researcherService.findByEmail( email );
            assertNotNull( r );
            assertNotNull( r.getContact() );
            assertEquals( email, r.getContact().getEmail() );
            assertEquals( department, r.getDepartment() );
            assertNotNull( userService.findByEmail( email ) );
            assertEquals( 0, r.getGenes().size() );
            assertEquals( 0, r.getTaxons().size() );
            assertEquals( 0, r.getPublications().size() );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testFindByGene() throws Exception {
        HashMap<Gene, TierType> genes = new HashMap<Gene, TierType>();
        genes.put( gene1, TierType.TIER2 );

        try {
            assertNotNull( researcher );
            Collection<Researcher> researchers = researcherService.findByGene( gene1 );
            assertEquals( 0, researchers.size() );
            assertTrue( researcherService.addGenes( researcher, genes ) );
            researchers = researcherService.findByGene( gene1 );
            assertEquals( 1, researchers.size() );
            researchers = researcherService.findByGene( gene2 );
            assertEquals( 0, researchers.size() );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testFindByUserName() throws Exception {

        try {
            assertNull( researcherService.findByUserName( "noUsername" ) );
            assertNotNull( researcher );
            Researcher r = researcherService.findByUserName( username );
            assertNotNull( r );
            assertNotNull( r.getContact() );
            assertEquals( email, r.getContact().getEmail() );
            assertEquals( department, r.getDepartment() );
            assertNotNull( userService.findByUserName( username ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testUpdateEmail() throws Exception {

        String newEmail = "newEmail@email.com";

        try {
            Researcher r = researcherService.findByEmail( email );
            User contact = r.getContact();
            assertNotNull( contact );
            assertEquals( email, contact.getEmail() );
            contact.setEmail( newEmail );
            researcherService.update( r );

            r = researcherService.findByEmail( email );
            assertNull( r );
            r = researcherService.findByEmail( newEmail );
            assertNotNull( r );
            assertEquals( newEmail, r.getContact().getEmail() );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testUpdateGene() throws Exception {
        HashMap<Gene, TierType> genes = new HashMap<Gene, TierType>();
        genes.put( gene1, TierType.TIER2 );

        HashMap<Gene, TierType> genes2 = new HashMap<Gene, TierType>();
        genes2.put( gene2, TierType.TIER2 );

        try {
            // gene hasn't been assigned yet
            assertEquals( 0, researcher.getGenes().size() );

            // now we assign it to the researcher
            assertTrue( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( "aaa", researcher.getGenes().iterator().next().getOfficialSymbol() );

            // now we update or replace the gene list
            assertTrue( researcherService.updateGenes( researcher, genes2 ) );
            assertEquals( "aaafish", researcher.getGenes().iterator().next().getOfficialSymbol() );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testAddRemoveGene() throws Exception {
        HashMap<Gene, TierType> genes = new HashMap<Gene, TierType>();
        genes.put( gene1, TierType.TIER2 );
        Collection<Gene> genesColl = new HashSet<>();
        genesColl.add( gene1 );

        try {

            // gene hasn't been assigned yet
            assertEquals( 0, researcher.getGenes().size() );

            // now we assign it to the researcher
            assertTrue( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( "aaa", researcher.getGenes().iterator().next().getOfficialSymbol() );

            // duplicate genes aren't allowed
            assertTrue( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( "aaa", researcher.getGenes().iterator().next().getOfficialSymbol() );

            // lets delete
            assertTrue( researcherService.removeGenes( researcher, genesColl ) );
            assertEquals( 0, researcher.getGenes().size() );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }
    }

    @Test
    public void testLoadAll() throws Exception {
        try {
            Collection<Researcher> researchers = researcherService.loadAll();
            // assertEquals( 1, researchers.size() );
            int controlSize = researchers.size();

            Researcher r = createResearcher( "testtesttest", "test@testtest.com", "testtesttest" );
            researchers = researcherService.loadAll();
            assertEquals( controlSize + 1, researchers.size() );

            researcherService.delete( r );
            researchers = researcherService.loadAll();
            assertEquals( controlSize, researchers.size() );
        } catch ( Exception e ) {
            e.printStackTrace();
            fail( e.getMessage() );
        }

    }
}
