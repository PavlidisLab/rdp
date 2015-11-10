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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.security.authentication.UserManager;

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
        String hql = "SELECT r FROM Researcher r INNER JOIN r.geneAssociations g WHERE g.pk.gene.id = :geneId";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql, "geneId", gene.getId() );
        return results;
    }

    @Override
    public Collection<Researcher> findByGene( final Gene gene, final TierType tier ) {
        String hql = "SELECT r FROM Researcher r INNER JOIN r.geneAssociations g WHERE ga.pk.gene.id = :geneId and g.tier = :tierType";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql,
                new String[] { "geneId", "tierType" }, new Object[] { gene.getId(), tier } );
        return results;
    }

    @Override
    public Collection<Researcher> findByLikeSymbol( final Long taxonId, final String symbol ) {
        String hql = "SELECT r FROM Researcher r INNER JOIN r.geneAssociations ga INNER JOIN ga.pk.gene g WHERE g.taxonId = :taxonId and g.officialSymbol LIKE ':symbol'";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql,
                new String[] { "taxonId", "symbol" }, new Object[] { taxonId, "%" + symbol + "%" } );
        return results;
    }

    @Override
    public Collection<Researcher> findByLikeSymbol( final Long taxonId, final String symbol, final TierType tier ) {
        String hql = "SELECT r FROM Researcher r INNER JOIN r.geneAssociations ga INNER JOIN g.pk.gene g WHERE g.taxonId = :taxonId and g.officialSymbol LIKE ':symbol' and ga.tier = :tierType";
        Collection<Researcher> results = this.getHibernateTemplate().findByNamedParam( hql,
                new String[] { "taxonId", "symbol", "tierType" }, new Object[] { taxonId, "%" + symbol + "%", tier } );
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
    public Long countResearchers() {
        String hql = "select count(*) FROM Researcher";
        return ( Long ) this.getHibernateTemplate().find( hql ).get( 0 );
    }

    @Override
    public Long countResearchersWithGenes() {
        String hql = "select count(distinct RESEARCHER_ID) FROM GeneAssociation";
        return ( Long ) this.getHibernateTemplate().find( hql ).get( 0 );
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
        // Hibernate.initialize( researcher.getGenes() );
        Hibernate.initialize( researcher.getTaxons() );
        Hibernate.initialize( researcher.getPublications() );
        Hibernate.initialize( researcher.getDescription() );
        Hibernate.initialize( researcher.getTaxonDescriptions() );

        return researcher;
    }
}
