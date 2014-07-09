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
package ubc.pavlab.rdp.server.dao;

import java.util.Collection;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.GroupAuthority;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup;

/**
 * TODO Document Me
 * 
 * @author ??
 * @version $Id: UserDao.java,v 1.5 2013/06/11 22:30:43 anton Exp $
 */
public interface UserDao extends DaoBase<User> {

    /**
     * 
     */
    public void addAuthority( User user, String roleName );

    /**
     * @param user
     * @param password - encrypted
     */
    public void changePassword( User user, String password );

    /**
     * @param contact
     * @return
     */
    public User find( User contact );

    /**
     * 
     */
    public User findByEmail( java.lang.String email );

    /**
     * 
     */
    public User findByUserName( java.lang.String userName );

    public Collection<GroupAuthority> loadGroupAuthorities( User u );

    public Collection<UserGroup> loadGroups( User user );

    public Collection<User> suggestUser( String queryString );
}
