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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateAccessor;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.GroupAuthority;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup;

//copied and modified from Gemma, some stuff should change

/**
* TODO Document Me
*
* @author ??
* @version $Id: UserDaoImpl.java,v 1.6 2013/06/11 22:30:45 anton Exp $
*/
@Repository
public class UserDaoImpl extends DaoBaseImpl<User> implements UserDao {

    @Autowired
    public UserDaoImpl( SessionFactory sessionFactory ) {
        super( User.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public void addAuthority( User user, String roleName ) {
        throw new UnsupportedOperationException( "User group-based authority instead" );
    }

    @Override
    public void changePassword( User user, String password ) {
        user.setPassword( password );
        this.getHibernateTemplate().update( user );
    }

    @Override
    public User find( User user ) {

        return this.findByUserName( user.getUserName() );
    }

    @Override
    public User findByUserName( final String userName ) {

        // we make this method safer to call in a transaction, as it really is a
        // read-only method that should be
        // accessing information that is already committed.
        HibernateTemplate t = new HibernateTemplate( this.getSessionFactory() );
        t.setAlwaysUseNewSession( true );
        t.setFlushMode( HibernateAccessor.FLUSH_NEVER );
        List<?> r = t.findByNamedParam( "from User u where lower(u.userName)=lower(:userName)", "userName", userName );
        if ( r.isEmpty() ) {
            return null;
        } else if ( r.size() > 1 ) {
            throw new IllegalStateException( "Multiple users with name=" + userName );
        }
        return ( User ) r.get( 0 );
    }

    @Override
    public java.util.Collection<? extends User> create( final java.util.Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.create - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            create( user );
        }
        return entities;
    }

    @Override
    public Collection<User> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from User where id in (:ids)", "ids", ids );
    }
    
        
    @Override
    public User create( final User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.create - 'user' can not be null" );
        }
        this.getHibernateTemplate().save( user );
        return user;

    }

    @Override
    public User findByEmail( final java.lang.String email ) {
        return this.findByEmail( "from User c where lower(c.email) = lower(:email)", email );
    }

    @Override
    public User load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( User.class, id );
        return ( User ) entity;
    }

    @Override
    public Collection<User> loadAll() {
        final Collection<User> results = this.getHibernateTemplate().loadAll( User.class );
        return results;
    }

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "User.remove - 'id' can not be null" );
        }
        User entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    @Override
    public void remove( Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.remove - 'user' can not be null" );
        }
        this.getHibernateTemplate().delete( user );
    }

    @Override
    public void update( final Collection<? extends User> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "User.update - 'entities' can not be null" );
        }
        for ( User user : entities ) {
            update( user );
        }
    }

    @Override
    public void update( User user ) {
        if ( user == null ) {
            throw new IllegalArgumentException( "User.update - 'user' can not be null" );
        }
        this.getHibernateTemplate().update( user );
    }

    private User findByEmail( final String queryString, final String email ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( email );
        argNames.add( "email" );
        Set<User> results = new LinkedHashSet<User>( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        User result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Contact" + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return result;
    }
    
    @Override
    public Collection<User> suggestUser(final String queryString){
    return this.getHibernateTemplate().findByNamedParam( "from User u where u.userName like '%"+queryString+"%", "userName", queryString );
    }
    
    

    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr.authorities from UserGroup gr inner join gr.groupMembers m where m = :user ", "user", u );
    }

    @Override
    public Collection<UserGroup> loadGroups( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select gr from UserGroup gr inner join gr.groupMembers m where m = :user ", "user", user );
    }

}