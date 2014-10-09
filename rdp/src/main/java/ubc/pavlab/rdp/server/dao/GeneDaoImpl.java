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

import java.util.ArrayList;
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

    /**
     * @see GeneDao#findByOfficalSymbol(String, String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Gene> findByOfficialSymbol( final String queryString, final String officialSymbol ) {
        java.util.List<String> argNames = new ArrayList<String>();
        java.util.List<Object> args = new ArrayList<Object>();
        args.add( officialSymbol );
        argNames.add( "officialSymbol" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<Gene> ) results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneDao#findBySmbol(java.lang.String)
     */
    @Override
    public Collection<Gene> findByOfficialSymbol( final String officialSymbol ) {
        return this.findByOfficialSymbol( "from Gene g where g.officialSymbol=:officialSymbol order by g.officialName",
                officialSymbol );
    }

    /**
     * @see GeneDao#findByOfficialSymbol(String, Taxon)
     */
    @Override
    public Gene findByOfficialSymbolAndTaxon( final String symbol, final String taxon ) {
        return this.handleFindByOfficialSymbol( symbol, taxon );

    }

    protected Gene handleFindByOfficialSymbol( String symbol, String taxon ) {
        // final String queryString =
        // "select distinct g from GeneImpl as g inner join g.taxon t where g.officialSymbol = :symbol and t= :taxon";
        final String queryString = "from Gene g where g.officialSymbol=:officialSymbol and g.taxon=:taxon";
        List<?> results = getHibernateTemplate().findByNamedParam( queryString,
                new String[] { "officialSymbol", "taxon" }, new Object[] { symbol, taxon } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match " + symbol + " in " + taxon + ", return first hit" );
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

    @Override
    public void updateGeneTable( String filePath ) {
        // This update query needs to have up to date field names
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

}
