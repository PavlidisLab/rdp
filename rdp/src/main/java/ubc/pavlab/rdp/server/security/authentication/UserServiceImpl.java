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

import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.authentication.UserExistsException;
import gemma.gsec.model.GroupAuthority;
import gemma.gsec.model.User;
import gemma.gsec.model.UserGroup;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.dao.UserDao;
import ubc.pavlab.rdp.server.dao.UserGroupDao;


/**
 * @author pavlidis
 * @version $Id: UserServiceImpl.java,v 1.4 2013/06/11 22:30:52 anton Exp $
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Autowired
    UserDao userDao;

    @Autowired
    UserGroupDao userGroupDao;

    @Autowired
    private AclService aclService;

    @Autowired
    private SecurityService securityService;

    @Override
    public void addGroupAuthority( UserGroup group, String authority ) {
        this.userGroupDao.addAuthority( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group,
                authority );
    }

    @Override
    public void addUserToGroup( UserGroup group, User user ) {
        // add user to list of members
        group.getGroupMembers().add( user );
        this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );

        // FIXME: Maybe user registration should be a completely separate, isolated code path.
        // Or maybe call to makeReadableByGroup shouldn't be here in the first place.
        // if (group.getName().equals( "Users" )) {
        // USERS group is a special case
        // } else {
        // grant read permissions to newly added user
        // this.securityService.makeReadableByGroup( group, group.getName() );
        // }
    }

    @Override
    public UserGroup create( UserGroup group ) {
        return this.userGroupDao.create( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
    }

    @Override
    public void delete( gemma.gsec.model.User user ) {
        for ( UserGroup group : this.userDao
                .loadGroups( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user ) ) {
            group.getGroupMembers().remove( user );
            this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
        }

        this.userDao.remove( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user );
    }

    @Override
    public Collection<ubc.pavlab.rdp.server.model.common.auditAndSecurity.User> suggestUser( String queryString ) {
        Collection<ubc.pavlab.rdp.server.model.common.auditAndSecurity.User> ret = new ArrayList<>();
        for ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User user : this.userDao
                .suggestUser( queryString ) ) {
            ret.add( user );
        }
        return ret;
    }

    @Override
    public void deleteByUserName( String userName ) {
        User user = findByUserName( userName );
        for ( UserGroup group : this.userDao
                .loadGroups( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user ) ) {
            group.getGroupMembers().remove( user );
            this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
        }

        this.userDao.remove( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user );
    }

    @Override
    public void delete( UserGroup group ) {
        String groupName = group.getName();

        if ( !groupExists( groupName ) ) {
            throw new IllegalArgumentException( "No group with that name: " + groupName );
        }

        /*
         * make sure this isn't one of the special groups - Administrators, Users, Agents
         */
        if ( groupName.equalsIgnoreCase( "Administrator" ) || groupName.equalsIgnoreCase( "Users" )
                || groupName.equalsIgnoreCase( "Agents" ) ) {
            throw new IllegalArgumentException( "Cannot delete that group, it is required for system operation." );
        }

        if ( !securityService.isOwnedByCurrentUser( findGroupByName( groupName ) ) ) {
            throw new AccessDeniedException( "Only the owner of a group can delete it" );
        }

        String authority = securityService.getGroupAuthorityNameFromGroupName( groupName );

        this.userGroupDao.remove( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );

        /*
         * clean up acls that use this group...do that last!
         */
        try {
            aclService.deleteSid( new AclGrantedAuthoritySid( authority ) );
        } catch ( DataIntegrityViolationException div ) {
            throw div;
        }
    }

    @Override
    public UserGroup findGroupByName( String name ) {
        return this.userGroupDao.findByUserGroupName( name );
    }

    @Override
    public Collection<UserGroup> findGroupsForUser( User user ) {
        Collection<UserGroup> ret = new ArrayList<>();
        for ( UserGroup group : this.userGroupDao
                .findGroupsForUser( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user ) ) {
            ret.add( group );
        }
        return ret;
    }

    @Override
    public Collection<UserGroup> listAvailableGroups() {
        Collection<UserGroup> ret = new ArrayList<>();
        for ( UserGroup group : this.userGroupDao.loadAll() ) {
            ret.add( group );
        }
        return ret;
    }

    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        Collection<GroupAuthority> ret = new ArrayList<>();
        for ( GroupAuthority groupAuthority : this.userDao
                .loadGroupAuthorities( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) u ) ) {
            ret.add( groupAuthority );
        }
        return ret;
    }

    @Override
    public void removeGroupAuthority( UserGroup group, String authority ) {
        this.userGroupDao.removeAuthority(
                ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group, authority );
    }

    @Override
    public void removeUserFromGroup( User user, UserGroup group ) {
        group.getGroupMembers().remove( user );
        this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );

        /*
         * TODO: if the group is empty, should we delete it? Not if it is GROUP_USER or ADMIN, but perhaps otherwise.
         */
    }

    @Override
    public void update( UserGroup group ) {
        this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
    }

    @Override
    public boolean groupExists( String name ) {
        return this.userGroupDao.findByUserGroupName( name ) != null;
    }

    /**
     * @see ubic.gemma.security.authentication.UserService#create(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public User create( final User user ) throws UserExistsException {

        if ( user.getUserName() == null ) {
            throw new IllegalArgumentException( "UserName cannot be null" );
        }

        if ( this.userDao.findByUserName( user.getUserName() ) != null ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

        if ( this.findByEmail( user.getEmail() ) != null ) {
            throw new UserExistsException( "A user with email address '" + user.getEmail() + "' already exists." );
        }

        try {
            return this.userDao.create( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user );
        } catch ( DataIntegrityViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        } catch ( InvalidDataAccessResourceUsageException e ) {
            // shouldn't happen if we don't have duplicates in the first place...but just in case.
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    /**
     * @see ubic.gemma.security.authentication.UserService#findByEmail(java.lang.String)
     */
    @Override
    public User findByEmail( final String email ) {
        return this.userDao.findByEmail( email );

    }

    /**
     * @see ubic.gemma.security.authentication.UserService#findByUserName(java.lang.String)
     */
    @Override
    public User findByUserName( final String userName ) {
        return this.userDao.findByUserName( userName );

    }

    /**
     * @see ubic.gemma.security.authentication.UserService#load(java.lang.Long)
     */
    @Override
    public User load( final Long id ) {
        return this.userDao.load( id );

    }

    /**
     * @see ubic.gemma.security.authentication.UserService#loadAll()
     */
    @Override
    public java.util.Collection<User> loadAll() {
        Collection<User> ret = new ArrayList<>();
        for ( User user : this.userDao.loadAll() ) {
            ret.add( user );
        }
        return ret;

    }

    /**
     * @see ubic.gemma.security.authentication.UserService#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    public void update( final User user ) {

        this.userDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User ) user );

    }

    @Override
    public void adminUpdate( ubc.pavlab.rdp.server.model.common.auditAndSecurity.User user ) {
        this.userDao.update( user );
    }

}