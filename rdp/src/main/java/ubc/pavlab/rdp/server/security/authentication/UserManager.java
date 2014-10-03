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

package ubc.pavlab.rdp.server.security.authentication;

import gemma.gsec.model.User;

import org.springframework.security.core.AuthenticationException;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.PasswordResetToken;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface UserManager extends gemma.gsec.authentication.UserManager {

    public PasswordResetToken createPasswordResetToken( User u );

    public PasswordResetToken getPasswordResetToken( String username );

    public boolean validatePasswordResetToken( String username, String key );

    public void changePasswordForUser( String username, String newPassword ) throws AuthenticationException;

    public void invalidatePasswordResetToken( String username );

    public void deleteUser( User user );

}
