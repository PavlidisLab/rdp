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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;

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

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.PasswordResetToken;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.security.authentication.UserManager;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Test that we can log users in
 * 
 * @author pavlidis
 * @version $Id: PrincipalTest.java,v 1.2 2012/10/09 18:29:54 cmcdonald Exp $
 */
public class PrincipalTest extends BaseSpringContextTest {

    private String pwd;

    private String username;

    private String email;

    private User user;

    @Autowired
    UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private void createUser() {
        pwd = randomName();
        username = randomName();
        email = username + "@foo.foo";

        String encodedPassword = passwordEncoder.encodePassword( pwd, username );
        UserDetailsImpl u = new UserDetailsImpl( encodedPassword, username, true, null, email, null, new Date() );
        userManager.createUser( u );
        user = ( User ) userManager.findByEmail( email );
    }

    @Before
    public void before() {
        createUser();
    }

    @Test
    public final void testResetPassword() throws Exception {
        User u = ( User ) userManager.findByUserName( username );
        String oldpwd = u.getPassword();
        String newpwd = randomName();
        String encodedPassword = passwordEncoder.encodePassword( newpwd, username );

        PasswordResetToken token = userManager.createPasswordResetToken( u );

        boolean valid = userManager.validatePasswordResetToken( username, token.getTokenKey() );
        assertTrue( valid );

        userManager.changePasswordForUser( username, encodedPassword );
        u = ( User ) userManager.findByUserName( username );
        assertEquals( encodedPassword, u.getPassword() );

        userManager.invalidatePasswordResetToken( username );
        try {
            valid = userManager.validatePasswordResetToken( username, token.getTokenKey() );
        } catch ( Exception e ) {
            valid = false;
        }
        assertFalse( valid );

    }

    @Test
    public final void testChangePassword() throws Exception {
        String newpwd = randomName();
        String encodedPassword = passwordEncoder.encodePassword( newpwd, username );

        // Current user changing password

        super.runAsUser( username );
        userManager.changePassword( pwd, encodedPassword );

        assertTrue( userManager.loadUserByUsername( username ).isEnabled() );
        assertEquals( encodedPassword, userManager.findByUserName( username ).getPassword() );

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
