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

package ubc.pavlab.rdp.server.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubc.pavlab.rdp.server.security.authentication.UserService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Tests the Group facilities of the UserManager..
 * 
 * @author keshav, paul
 * @version $Id: UserGroupServiceTest.java,v 1.5 2013/03/01 21:46:22 cmcdonald Exp $
 */
public class UserGroupServiceTest extends BaseSpringContextTest {

    @Autowired
    private UserManager userManager = null;

    @Autowired
    private UserService userService = null;

    @Autowired
    private SecurityService securityService = null;

    private String groupName = null;

    private String userName1 = "jonesey";

    private String userName2 = "mark";

    @Before
    public void setup() throws Exception {
        this.groupName = RandomStringUtils.randomAlphabetic( 6 );

        /*
         * Create a user with default privileges.
         */
        try {
            this.userManager.loadUserByUsername( this.userName1 );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", this.userName1, true, null, "foo@gmail.com",
                    "key", new Date() ) );
        }

        try {
            this.userManager.loadUserByUsername( this.userName2 );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo2", this.userName2, true, null, "foo2@gmail.com",
                    "key2", new Date() ) );
        }
    }

    /**
     * Tests creating a UserGroup
     */
    @Test
    public void testCreateUserGroup() {

        List<GrantedAuthority> authos = new ArrayList<GrantedAuthority>();
        authos.add( new GrantedAuthorityImpl( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = this.userManager.findGroupAuthorities( this.groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

    }

    /**
     * Test for deleting a user group
     */
    @Test
    public void testDeleteUserGroup() {

        runAsAdmin();
        List<GrantedAuthority> authos = new ArrayList<GrantedAuthority>();
        authos.add( new GrantedAuthorityImpl( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        // add another user to group
        this.userManager.addUserToGroup( this.userName1, this.groupName );
        this.userManager.addUserToGroup( this.userName2, this.groupName );

        // delete the group
        this.userManager.deleteGroup( this.groupName );

    }

    /**
     * Tests updating the UserGroup
     */
    @Test
    public void testUpdateUserGroup() {
        List<GrantedAuthority> authos = new ArrayList<GrantedAuthority>();
        authos.add( new GrantedAuthorityImpl( "GROUP_TESTING" ) );
        this.userManager.createGroup( this.groupName, authos );

        List<GrantedAuthority> findGroupAuthorities = this.userManager.findGroupAuthorities( this.groupName );

        for ( GrantedAuthority grantedAuthority : findGroupAuthorities ) {
            assertEquals( "GROUP_TESTING", grantedAuthority.getAuthority() );
        }

        /*
         * Add a user to the group
         */

        this.userManager.addUserToGroup( this.userName1, this.groupName );

        List<String> users = this.userManager.findUsersInGroup( this.groupName );
        assertTrue( users.contains( this.userName1 ) );

        /*
         * Make sure user can see group (from bug 2822)
         */
        gemma.gsec.model.UserGroup group = this.userService.findGroupByName( this.groupName );
        this.securityService.isViewableByUser( group, this.userName1 );

        /*
         * Remove a user from the group.
         */
        this.userManager.removeUserFromGroup( this.userName1, this.groupName );
        users = this.userManager.findUsersInGroup( this.groupName );
        assertTrue( !users.contains( this.userName1 ) );

        super.runAsUser( this.userName1 );

        /*
         * Can the user remove themselves from the group?
         */
        try {
            this.userManager.removeUserFromGroup( this.userName1, this.groupName );
            fail( "Should have gotten access denied when user tried to remove themselves from a group" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }

        /*
         * Can they elevate the group authority?
         */
        try {
            this.userManager.addGroupAuthority( this.groupName, new GrantedAuthorityImpl( "GROUP_ADMIN" ) );
            fail( "Should have gotten access denied when user tried to make group ADMIN" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }

    }

    @Test
    public void testUserAddSelvesToAdmin() {
        super.runAsUser( this.userName1 );

        try {
            this.userManager.addUserToGroup( this.userName1, "Administrators" );
            fail( "Should have gotten access denied when user tried to make themselves admin" );
        } catch ( AccessDeniedException ok ) {
            // expected behaviour
        }
    }

}
