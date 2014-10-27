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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.Taxon;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Repository
public class TaxonDaoImpl extends DaoBaseImpl<Taxon> implements TaxonDao {

    protected final Log log = LogFactory.getLog( UserGroupDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public TaxonDaoImpl( SessionFactory sessionFactory ) {
        super( Taxon.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.TaxonDao#findById(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Taxon findById( Long id ) {
        String hql = "FROM Taxon t WHERE t.id = :id";
        Collection<Taxon> results = this.getHibernateTemplate().findByNamedParam( hql, "id", id );
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Taxon" + "' was found when executing query --> '" + hql + "'" );
        } else if ( results.size() == 0 ) {
            return null;
        }
        return results.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.TaxonDao#findByCommonName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Taxon findByCommonName( String commonName ) {
        String hql = "FROM Taxon t WHERE t.commonName = :commonName";
        Collection<Taxon> results = this.getHibernateTemplate().findByNamedParam( hql, "commonName", commonName );
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'Taxon" + "' was found when executing query --> '" + hql + "'" );
        } else if ( results.size() == 0 ) {
            return null;
        }
        return results.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.TaxonDao#findByCommonName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<Long> loadAllIds() {
        String hql = "SELECT id FROM Taxon t";
        Collection<Long> results = this.getHibernateTemplate().find( hql );
        return results;
    }

}
