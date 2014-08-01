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
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserService;
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
}
