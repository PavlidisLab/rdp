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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
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
    private String email = "foobar@email.com";
    private String username = "foobar";
    private String department = "dept";

    @Before
    public void setUp() {
        researcher = researcherService.findByEmail( email );
        researcherService.delete( researcher );
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

            researcher = createResearcher( username, email, department );
            assertNotNull( researcher );
            researcher = researcherService.findByEmail( email );
            assertNotNull( researcher );
            assertNotNull( researcher.getContact() );
            assertEquals( email, researcher.getContact().getEmail() );
            assertEquals( department, researcher.getDepartment() );
            assertNotNull( userService.findByEmail( email ) );
            assertEquals( 0, researcher.getGenes().size() );
            assertEquals( 0, researcher.getTaxons().size() );
            assertEquals( 0, researcher.getPublications().size() );
        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            assertNull( researcherService.findByEmail( email ) );
            fail( e.getMessage() );
        }
        researcherService.delete( researcher );
        assertNull( researcherService.findByEmail( email ) );
        assertNull( userService.findByEmail( email ) );
    }

    @Test
    public void testFindByGene() throws Exception {

        Gene gene = new Gene();
        gene.setEnsemblId( "ENSG123" );
        gene.setOfficialSymbol( "ABC" );
        // geneService.create( gene );
        Collection<Gene> genes = new HashSet<>();
        genes.add( gene );

        try {
            researcher = createResearcher( username, email, department );
            assertNotNull( researcher );
            Collection<Researcher> researchers = researcherService.findByGene( gene );
            assertEquals( 0, researchers.size() );
            assertTrue( researcherService.addGenes( researcher, genes ) );
            researchers = researcherService.findByGene( gene );
            assertEquals( 1, researchers.size() );
        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            assertNull( researcherService.findByUserName( username ) );
            fail( e.getMessage() );
        }
        researcherService.delete( researcher );
    }

    @Test
    public void testFindByUserName() throws Exception {

        try {
            assertNull( researcherService.findByUserName( "noUsername" ) );

            researcher = createResearcher( username, email, department );
            assertNotNull( researcher );
            researcher = researcherService.findByUserName( username );
            assertNotNull( researcher );
            assertNotNull( researcher.getContact() );
            assertEquals( email, researcher.getContact().getEmail() );
            assertEquals( department, researcher.getDepartment() );
            assertNotNull( userService.findByUserName( username ) );
        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            assertNull( researcherService.findByUserName( username ) );
            fail( e.getMessage() );
        }
        researcherService.delete( researcher );
        assertNull( researcherService.findByUserName( username ) );
        assertNull( userService.findByUserName( username ) );
    }

    @Test
    public void testSaveFirstName() throws Exception {

        String newEmail = "newEmail@email.com";

        try {
            researcher = createResearcher( username, email, department );
            User contact = researcher.getContact();
            assertNotNull( contact );
            assertEquals( email, contact.getEmail() );
            contact.setEmail( newEmail );
            researcherService.update( researcher );
            contact = ( User ) userManager.findbyEmail( newEmail );
            assertNotNull( contact );
            assertEquals( newEmail, contact.getEmail() );
        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            fail( e.getMessage() );
        }

        researcherService.delete( researcher );
    }

    @Test
    public void testUpdateGene() throws Exception {
        String officialSymbol = "GENEA";
        Gene gene = new Gene( officialSymbol );
        Collection<Gene> genes = new ArrayList<>();
        genes.add( gene );

        String officialSymbol2 = "GENE2";
        Gene gene2 = new Gene( officialSymbol2 );
        Collection<Gene> genes2 = new ArrayList<>();
        genes2.add( gene2 );

        try {
            // gene hasn't been created yet
            researcher = createResearcher( username, email, department );
            assertEquals( 0, researcher.getGenes().size() );
            assertEquals( 0, geneService.findByOfficalSymbol( officialSymbol ).size() );

            // now we create the gene and assign it to the researcher
            assertTrue( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( officialSymbol, researcher.getGenes().iterator().next().getOfficialSymbol() );
            assertEquals( 1, geneService.findByOfficalSymbol( officialSymbol ).size() );

            // now we update or replace the gene list
            assertTrue( researcherService.updateGenes( researcher, genes2 ) );
            assertEquals( officialSymbol2, researcher.getGenes().iterator().next().getOfficialSymbol() );

        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            fail( e.getMessage() );
        }

        researcherService.delete( researcher );
        geneService.delete( gene );
    }

    @Test
    public void testAddRemoveGene() throws Exception {
        String officialSymbol = "GENEA";
        Gene gene = new Gene( officialSymbol );
        Collection<Gene> genes = new ArrayList<>();
        genes.add( gene );

        try {

            // gene hasn't been created yet
            researcher = createResearcher( username, email, department );
            assertEquals( 0, researcher.getGenes().size() );
            assertEquals( 0, geneService.findByOfficalSymbol( officialSymbol ).size() );

            // now we create the gene and assign it to the researcher
            assertTrue( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( officialSymbol, researcher.getGenes().iterator().next().getOfficialSymbol() );
            assertEquals( 1, geneService.findByOfficalSymbol( officialSymbol ).size() );

            // duplicate genes aren't allowed
            assertFalse( researcherService.addGenes( researcher, genes ) );
            assertEquals( 1, researcher.getGenes().size() );
            assertEquals( officialSymbol, researcher.getGenes().iterator().next().getOfficialSymbol() );
            assertEquals( 1, geneService.findByOfficalSymbol( officialSymbol ).size() );

            // lets delete
            assertTrue( researcherService.removeGenes( researcher, genes ) );
            assertEquals( 0, researcher.getGenes().size() );
            assertEquals( 1, geneService.findByOfficalSymbol( officialSymbol ).size() ); // keep the gene in case other
                                                                                         // researchers use it
        } catch ( Exception e ) {
            e.printStackTrace();
            researcherService.delete( researcher );
            fail( e.getMessage() );
        }

        researcherService.delete( researcher );
        geneService.delete( gene );
    }
}
