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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Taxon;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Repository
public class GeneDaoImpl extends DaoBaseImpl<Gene> implements GeneDao {

    protected final Log log = LogFactory.getLog( GeneDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public GeneDaoImpl( SessionFactory sessionFactory ) {
        super( Gene.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneDao#findBySmbol(java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        final String queryString = "from Gene g where g.officialSymbol=:officialSymbol";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "officialSymbol" },
                new Object[] { officialSymbol } );
        if ( results.size() == 0 ) {
            return null;
        } else {
            return ( Collection<Gene> ) results;
        }
    }

    /**
     * @see GeneDao#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbolAndTaxon( final String symbol, final Long taxonId ) {
        return this.handleFindByOfficialSymbol( symbol, taxonId );

    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByTaxonId( Long taxonId ) {
        final String queryString = "from Gene g where g.taxonId=:taxonId";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "taxonId" },
                new Object[] { taxonId } );
        if ( results.size() == 0 ) {
            return null;
        } else {
            return ( Collection<Gene> ) results;
        }

    }

    protected Gene handleFindByOfficialSymbol( String symbol, Long taxonId ) {
        // final String queryString =
        // "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        final String queryString = "from Gene g where g.officialSymbol=:officialSymbol and g.taxonId=:taxonId";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString,
                new String[] { "officialSymbol", "taxonId" }, new Object[] { symbol, taxonId } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + symbol + " in " + taxonId + ", return first hit" );
        }
        return ( Gene ) results.iterator().next();
    }

    @Override
    public Gene findById( final Long id ) {
        final String queryString = "from Gene g where g.id=:id";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "id" },
                new Object[] { id } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + id + ", return first hit" );
        }
        return ( Gene ) results.iterator().next();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<Gene> findByMultipleIds( final Collection<Long> ids ) {
        final String queryString = "from Gene g where g.id in (:ids)";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString, new String[] { "ids" },
                new Object[] { ids } );
        return ( Collection<Gene> ) results;
    }

    @Override
    public void updateGeneTable( String filePath ) {
        // This query needs to have up to date field names in our table and orderings from the data file
        getHibernateTemplate()
                .getSessionFactory()
                .getCurrentSession()
                .createSQLQuery(
                        "LOAD DATA LOCAL INFILE :fileName INTO TABLE GENE IGNORE 1 LINES "
                                + "(tax_id, GeneID, Symbol, @dummy, @Synonyms, @dummy, "
                                + "@dummy, @dummy, @description, @dummy, @dummy, @dummy, "
                                + "@dummy, @dummy, Modification_date) "
                                + "SET Synonyms = IF(@Synonyms='-', NULL, @Synonyms), "
                                + "description = IF(@description='-', NULL, @description);" )
                .setParameter( "fileName", filePath ).executeUpdate();

    }

    @Override
    public void truncateGeneTable() {
        // Removing Foreign Key Constraints is usually a bad idea for data integrity
        // however, since the FK GeneID is unique outside of our application as well
        // it is safe to assume that data integrity will be kept with the newly loaded
        // database.

        getHibernateTemplate().getSessionFactory().getCurrentSession().createSQLQuery( "SET FOREIGN_KEY_CHECKS=0;" )
                .executeUpdate();

        getHibernateTemplate().getSessionFactory().getCurrentSession().createSQLQuery( "TRUNCATE TABLE GENE;" )
                .executeUpdate();

        getHibernateTemplate().getSessionFactory().getCurrentSession().createSQLQuery( "SET FOREIGN_KEY_CHECKS=1;" )
                .executeUpdate();

    }

    @Override
    public Long countAssociations() {
        String hql = "select count(*) FROM GeneAssociation WHERE tier<>'TIER3'";
        return ( Long ) this.getHibernateTemplate().find( hql ).get( 0 );
    }

    @Override
    public Long countUniqueAssociations() {
        String hql = "select count(distinct GeneID) FROM GeneAssociation WHERE tier<>'TIER3'";
        return ( Long ) this.getHibernateTemplate().find( hql ).get( 0 );
    }

}
