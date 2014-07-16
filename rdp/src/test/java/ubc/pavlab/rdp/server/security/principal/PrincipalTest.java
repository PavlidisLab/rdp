/*
 * The aspiredb project
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

package ubc.pavlab.rdp.server.security.principal;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Test that we can log users in
 * 
 * @author pavlidis
 * @version $Id: PrincipalTest.java,v 1.2 2012/10/09 18:29:54 cmcdonald Exp $
 */
public class PrincipalTest extends BaseSpringContextTest {

    String pwd;

    String username;

    @Autowired
    UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private String email;

    @Before
    public void before() {

        pwd = randomName();
        username = randomName();
        email = username + "@foo.foo";

        try {
            userManager.loadUserByUsername( username );
        } catch ( UsernameNotFoundException e ) {

            String encodedPassword = passwordEncoder.encodePassword( pwd, username );
            UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, true, null, email, null, new Date() );

            log.error( "Encoded password old password " + pwd + " encoded is " + encodedPassword + " user is "
                    + username );

            userManager.createUser( u );
        }
    }

    @Test
    public final void testChangePassword() throws Exception {
        String oldpwd = userManager.findByUserName( username ).getPassword();
        String newpwd = randomName();
        String encodedPassword = passwordEncoder.encodePassword( newpwd, username );

        String token = userManager.changePasswordForUser( email, username, encodedPassword );

        assertTrue( !userManager.loadUserByUsername( username ).isEnabled() );

        /*
         * User has to unlock the account, we mimic that:
         */
        assertTrue( userManager.validateSignupToken( username, token ) );

        assertTrue( userManager.loadUserByUsername( username ).isEnabled() );

        Authentication auth = new UsernamePasswordAuthenticationToken( username, newpwd );
        Authentication authentication = ( ( ProviderManager ) authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );

        assertNotSame( oldpwd, userManager.findByUserName( username ).getPassword() );

        // Now that the account has been activated, try changing the password
        oldpwd = newpwd;
        newpwd = randomName();
        encodedPassword = passwordEncoder.encodePassword( newpwd, username );

        // Current user changing password
        super.runAsUser( username );
        userManager.changePassword( oldpwd, encodedPassword );

        assertTrue( userManager.loadUserByUsername( username ).isEnabled() );
        assertNotSame( oldpwd, userManager.findByUserName( username ).getPassword() );
        auth = new UsernamePasswordAuthenticationToken( username, newpwd );
        authentication = ( ( ProviderManager ) authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testLogin() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, pwd );

        Authentication authentication = ( ( ProviderManager ) authenticationManager ).authenticate( auth );
        assertTrue( authentication.isAuthenticated() );
    }

    @Test
    public final void testLoginNonexistentUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( "bad user", "wrong password" );

        try {
            ( ( ProviderManager ) authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

    @Test
    public final void testLoginWrongPassword() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken( username, "wrong password" );

        try {
            ( ( ProviderManager ) authenticationManager ).authenticate( auth );
            fail( "Should have gotten a bad credentials exception" );
        } catch ( BadCredentialsException e ) {
            //
        }
    }

}
