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

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Repository
public class GeneAnnotationDaoImpl extends DaoBaseImpl<GeneAnnotation> implements GeneAnnotationDao {

    protected final Log log = LogFactory.getLog( GeneAnnotationDaoImpl.class );

    /**
     * @param sessionFactory
     */
    @Autowired
    public GeneAnnotationDaoImpl( SessionFactory sessionFactory ) {
        super( GeneAnnotation.class );
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneAnnotationDao#findByGeneOntologyId(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneAnnotation> findByGeneOntologyId( String geneOntologyId ) {
        String hql = "FROM GeneAnnotation ga WHERE ga.pk.geneOntologyId = :geneOntologyId";
        Collection<GeneAnnotation> results = this.getHibernateTemplate().findByNamedParam( hql, "geneOntologyId",
                geneOntologyId );
        if ( results.size() == 0 ) {
            return null;
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneAnnotationDao#findByGene(ubc.pavlab.rdp.server.model.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneAnnotation> findByGene( Gene gene ) {
        String hql = "FROM GeneAnnotation ga WHERE ga.pk.gene.id = :geneId";
        Collection<GeneAnnotation> results = this.getHibernateTemplate().findByNamedParam( hql, "geneId", gene.getId() );
        if ( results.size() == 0 ) {
            return null;
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneAnnotationDao#findByGeneAndGeneOntologyId(ubc.pavlab.rdp.server.model.Gene,
     * java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public GeneAnnotation findByGeneAndGeneOntologyId( Gene gene, String geneOntologyId ) {
        String hql = "FROM GeneAnnotation ga WHERE ga.pk.gene.id = :geneId AND ga.pk.geneOntologyId = :geneOntologyId";
        Collection<GeneAnnotation> results = this.getHibernateTemplate().findByNamedParam( hql,
                new String[] { "geneId", "geneOntologyId" }, new Object[] { gene.getId(), geneOntologyId } );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple genes match geneId: (" + gene.getId() + ") and GO_ID: (" + geneOntologyId
                    + "), return first hit" );
        }
        return ( GeneAnnotation ) results.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.dao.GeneAnnotationDao#countGenesForGeneOntologyId(java.lang.String)
     */
    @Override
    public Long countGenesForGeneOntologyId( String geneOntologyId ) {
        String hql = "select count(*) FROM GeneAnnotation ga WHERE ga.pk.geneOntologyId = :geneOntologyId";
        Long count = ( Long ) this.getHibernateTemplate().findByNamedParam( hql, "geneOntologyId", geneOntologyId )
                .get( 0 );
        return count;
    }

    @Override
    public void updateGeneAnnotationTable( String filePath ) {
        // This query needs to have up to date field names in our table and orderings from the data file
        getHibernateTemplate()
                .getSessionFactory()
                .getCurrentSession()
                .createSQLQuery(
                        "LOAD DATA LOCAL INFILE :fileName INTO TABLE GENE_ANNOTATION IGNORE 1 LINES "
                                + "(TAXON_ID, GeneID, geneOntologyId, EVIDENCE, @QUALIFIER, GO_TERM, "
                                + "@PUBMED, CATEGORY) SET QUALIFIER = IF(@QUALIFIER='-', NULL, @QUALIFIER), "
                                + "PUBMED = IF(@PUBMED='-', NULL, @PUBMED);" ).setParameter( "fileName", filePath )
                .executeUpdate();

    }

    @Override
    public void truncateGeneAnnotationTable() {
        // Removing Foreign Key Constraints is usually a bad idea for data integrity
        // however, since the FK GeneID is unique outside of our application as well
        // it is safe to assume that data integrity will be kept with the newly loaded
        // database.

        getHibernateTemplate().getSessionFactory().getCurrentSession().createSQLQuery( "SET FOREIGN_KEY_CHECKS=0;" )
                .executeUpdate();

        getHibernateTemplate().getSessionFactory().getCurrentSession()
                .createSQLQuery( "TRUNCATE TABLE GENE_ANNOTATION;" ).executeUpdate();

        getHibernateTemplate().getSessionFactory().getCurrentSession().createSQLQuery( "SET FOREIGN_KEY_CHECKS=1;" )
                .executeUpdate();

    }

}
