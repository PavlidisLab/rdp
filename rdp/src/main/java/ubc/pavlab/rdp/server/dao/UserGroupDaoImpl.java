/*
 * The aspiredb project
 * 
 * Copyright (c) 2014 University of British Columbia
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.GroupAuthority;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup;

/**
 * TODO document me
 * 
 * @author ????
 */
@Repository
public class UserGroupDaoImpl extends DaoBaseImpl<UserGroup> implements UserGroupDao {

    protected final Log log = LogFactory.getLog( UserGroupDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public UserGroupDaoImpl( SessionFactory sessionFactory ) {
        super( UserGroup.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubc.pavlab.aspiredb.server.dao.UserGroupDao#addAuthority(ubc.pavlab.aspiredb.server.model.common.auditAndSecurity
     * .UserGroup, java.lang.String)
     */
    @Override
    public void addAuthority( UserGroup group, String authority ) {

        for ( gemma.gsec.model.GroupAuthority ga : group.getAuthorities() ) {
            if ( ga.getAuthority().equals( authority ) ) {
                log.warn( "Group already has authority " + authority );
                return;
            }
        }

        GroupAuthority ga = new GroupAuthority();
        ga.setAuthority( authority );

        group.getAuthorities().add( ga );

        this.getHibernateTemplate().update( group );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubc.pavlab.aspiredb.server.dao.UserGroupDao#addToGroup(ubc.pavlab.aspiredb.server.model.common.auditAndSecurity
     * .UserGroup, ubc.pavlab.aspiredb.server.model.common.auditAndSecurity.User)
     */
    @Override
    public void addToGroup( UserGroup group, User user ) {
        group.getGroupMembers().add( user );
        this.getHibernateTemplate().update( group );
    }

    @Override
    public UserGroup findByUserGroupName( final java.lang.String name ) {
        return this.findByUserGroupName( "from UserGroup as userGroup where userGroup.name = :name", name );
    }

    @Override
    public Collection<UserGroup> findGroupsForUser( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ug from UserGroup ug join ug.groupMembers memb where memb = :user", "user", user );
    }

    @Override
    public void removeAuthority( UserGroup group, String authority ) {

        for ( Iterator<GroupAuthority> iterator = group.getAuthorities().iterator(); iterator.hasNext(); ) {
            gemma.gsec.model.GroupAuthority ga = iterator.next();
            if ( ga.getAuthority().equals( authority ) ) {
                iterator.remove();
            }
        }

        this.getHibernateTemplate().update( group );
    }

    /**
     * @param queryString
     * @param name
     * @return user group name
     */
    private UserGroup findByUserGroupName( final java.lang.String queryString, final java.lang.String name ) {
        Set<UserGroup> results = new LinkedHashSet<UserGroup>( this.getHibernateTemplate().findByNamedParam(
                queryString, new String[] { "name" }, new Object[] { name } ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'UserGroup" + "' was found when executing query --> '" + queryString
                            + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( UserGroup ) result;
    }
}