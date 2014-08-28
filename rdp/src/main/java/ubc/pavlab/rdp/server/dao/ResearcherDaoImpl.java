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

package ubc.pavlab.rdp.server.dao;

import gemma.gsec.authentication.UserManager;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Researcher;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Repository
public class ResearcherDaoImpl extends DaoBaseImpl<Researcher> implements ResearcherDao {

    @Autowired
    UserManager userManager;

    protected final Log log = LogFactory.getLog( UserGroupDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public ResearcherDaoImpl( SessionFactory sessionFactory ) {
        super( Researcher.class );
        super.setSessionFactory( sessionFactory );
    }

    @Override
    public Researcher findByEmail( final String email ) {
        Researcher researcher = null;
        String hql = "FROM Researcher r WHERE r.contact.email = :email";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql, "email", email );
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Researcher" + "' was found when executing query --> '" + hql + "'" );
        } else if ( results.size() == 0 ) {
            return null;
        } else {
            researcher = results.iterator().next();
        }
        return researcher;
    }

    @Override
    public Collection<Researcher> findByGene( final Gene gene ) {
        String hql = "FROM Researcher r INNER JOIN r.genes g WITH g.id = :geneId";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql, "geneId", gene.getId() );
        return results;
    }

    @Override
    public Researcher findByUsername( final String username ) {
        String hql = "FROM Researcher r WHERE r.contact.userName = :userName";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql, "userName", username );
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Researcher" + "' was found when executing query --> '" + hql + "'" );
        } else if ( results.size() == 0 ) {
            return null;
        }
        return results.iterator().next();
    }

    @Override
    public Researcher thaw( Researcher researcher ) {
        if ( researcher == null ) {
            return null;
        }

        if ( researcher.getId() == null ) {
            throw new IllegalArgumentException( "Id cannot be null, cannot be thawed: " + researcher );
        }

        Hibernate.initialize( researcher.getContact() );
        Hibernate.initialize( researcher.getGenes() );
        Hibernate.initialize( researcher.getTaxons() );
        Hibernate.initialize( researcher.getPublications() );

        return researcher;
    }
}
