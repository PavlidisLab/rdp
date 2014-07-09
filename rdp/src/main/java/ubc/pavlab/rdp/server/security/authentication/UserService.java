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

package ubc.pavlab.rdp.server.security.authentication;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;

/**
 * @version $Id: UserService.java,v 1.4 2013/06/11 22:30:51 anton Exp $
 * @author paul
 */
public interface UserService extends gemma.gsec.authentication.UserService {

    /**
     * Remove a user from the persistent store.
     * 
     * @param user
     */
    @Secured({ "GROUP_ADMIN" })
    public void deleteByUserName( String userName );

    public Collection<User> suggestUser( String queryString );

    @Secured({ "GROUP_ADMIN" })
    public void adminUpdate( User user );

}
