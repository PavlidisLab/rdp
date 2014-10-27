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

package ubc.pavlab.rdp.server.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.GeneAnnotationDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service("geneAnnotationService")
public class GeneAnnotationServiceImpl implements GeneAnnotationService {

    private static Log log = LogFactory.getLog( GeneAnnotationServiceImpl.class );

    @Autowired
    GeneAnnotationDao geneAnnotationDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#create(ubc.pavlab.rdp.server.model.GeneAnnotation)
     */
    @Override
    public GeneAnnotation create( GeneAnnotation geneAnnotation ) {
        return geneAnnotationDao.create( geneAnnotation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#delete(ubc.pavlab.rdp.server.model.GeneAnnotation)
     */
    @Override
    public void delete( GeneAnnotation geneAnnotation ) {
        geneAnnotationDao.remove( geneAnnotation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#findByGeneOntologyId(java.lang.String)
     */
    @Transactional
    @Override
    public Collection<GeneAnnotation> findByGeneOntologyId( String geneOntologyId ) {
        return geneAnnotationDao.findByGeneOntologyId( geneOntologyId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#findByGeneOntologyIdAndTaxon(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public Collection<GeneAnnotation> findByGeneOntologyIdAndTaxon( String geneOntologyId, Long taxonId ) {
        return geneAnnotationDao.findByGeneOntologyIdAndTaxon( geneOntologyId, taxonId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#findByGene(ubc.pavlab.rdp.server.model.Gene)
     */
    @Transactional
    @Override
    public Collection<GeneAnnotation> findByGene( Gene gene ) {
        return geneAnnotationDao.findByGene( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubc.pavlab.rdp.server.service.GeneAnnotationService#findByGeneAndGeneOntologyId(ubc.pavlab.rdp.server.model.Gene,
     * java.lang.String)
     */
    @Transactional
    @Override
    public GeneAnnotation findByGeneAndGeneOntologyId( Gene gene, String geneOntologyId ) {
        return geneAnnotationDao.findByGeneAndGeneOntologyId( gene, geneOntologyId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneAnnotationService#countGenesForGeneOntologyId(java.lang.String)
     */
    @Transactional
    @Override
    public Long countGenesForGeneOntologyId( String geneOntologyId ) {
        return geneAnnotationDao.countGenesForGeneOntologyId( geneOntologyId );
    }

    @Transactional
    @Override
    public Long countGenesForGeneOntologyIdAndTaxon( String geneOntologyId, Long taxonId ) {
        return geneAnnotationDao.countGenesForGeneOntologyIdAndTaxon( geneOntologyId, taxonId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubc.pavlab.rdp.server.service.GeneAnnotationService#findByGeneLimitedByTermSize(ubc.pavlab.rdp.server.model.Gene,
     * int)
     */
    @Transactional
    @Override
    public Collection<GeneAnnotation> findByGeneLimitedByTermSize( Gene gene, int limit ) {
        Collection<GeneAnnotation> genesAnnotations = findByGene( gene );
        for ( Iterator<GeneAnnotation> i = genesAnnotations.iterator(); i.hasNext(); ) {
            GeneAnnotation ga = i.next();
            // This might be slow...
            Long count = countGenesForGeneOntologyId( ga.getPk().getGeneOntologyId() );
            if ( count > limit ) {
                i.remove();
            }
        }

        return genesAnnotations;

    }

    /**
     * @param genes the collection of genes to find terms for
     * @return A mapping of terms to their frequency among the genes
     */
    @Deprecated
    @Override
    public Map<GeneOntologyTerm, Long> findTermsAndFrequenciesByGenes( Collection<Gene> genes ) {
        Map<GeneOntologyTerm, Long> geneTermMap = new HashMap<GeneOntologyTerm, Long>();
        for ( Gene g : genes ) {
            Collection<GeneAnnotation> geneAnnotations = findByGene( g );
            for ( GeneAnnotation ga : geneAnnotations ) {
                GeneOntologyTerm goTerm = new GeneOntologyTerm( ga.getGeneOntologyId(), ga.getGeneOntologyTerm() );
                if ( geneTermMap.containsKey( goTerm ) ) {
                    geneTermMap.put( goTerm, geneTermMap.get( goTerm ) + 1 );
                } else {
                    geneTermMap.put( goTerm, 1L );
                }
            }
        }
        return geneTermMap;
    }

    /**
     * @param genes the collection of genes to find terms for
     * @param limit maximum genes associated with any returned terms
     * @return A mapping of terms to their frequency among the genes where each term has a maximum of <code>limit</code>
     *         genes associated with it
     */
    @Deprecated
    @Override
    public Map<GeneOntologyTerm, Long> findTermsAndFrequenciesByGenesLimitedByTermSize( Collection<Gene> genes,
            int limit ) {
        Map<GeneOntologyTerm, Long> goTerms = findTermsAndFrequenciesByGenes( genes );
        for ( Iterator<Map.Entry<GeneOntologyTerm, Long>> i = goTerms.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<GeneOntologyTerm, Long> goTerm = i.next();
            // This might be slow...
            Long count = countGenesForGeneOntologyId( goTerm.getKey().getGeneOntologyId() );
            if ( count > limit ) {
                i.remove();
            }
        }
        return goTerms;

    }

    /**
     * @param genes the collection of genes to find terms for
     * @return A set of terms among the genes
     */
    @Deprecated
    @Override
    public Collection<GeneOntologyTerm> findTermsByGenes( Collection<Gene> genes ) {
        Collection<GeneOntologyTerm> results = new HashSet<GeneOntologyTerm>();

        for ( Gene g : genes ) {
            Collection<GeneAnnotation> geneAnnotations = findByGene( g );
            for ( GeneAnnotation ga : geneAnnotations ) {
                results.add( new GeneOntologyTerm( ga.getGeneOntologyId(), ga.getGeneOntologyTerm() ) );
            }
        }
        return results;
    }

    /**
     * @param genes the collection of genes to find terms for
     * @param limit maximum genes associated with any returned terms
     * @return A set of terms among the genes where each term has a maximum of <code>limit</code> genes associated with
     *         it
     */
    @Deprecated
    @Override
    public Collection<GeneOntologyTerm> findTermsByGenesLimitedByTermSize( Collection<Gene> genes, int limit ) {
        Collection<GeneOntologyTerm> goTerms = findTermsByGenes( genes );
        for ( Iterator<GeneOntologyTerm> i = goTerms.iterator(); i.hasNext(); ) {
            GeneOntologyTerm goTerm = i.next();
            // This might be slow...
            Long count = countGenesForGeneOntologyId( goTerm.getGeneOntologyId() );
            if ( count > limit ) {
                i.remove();
            }
        }

        return goTerms;
    }

    /**
     * @param genes the collection of genes to find related terms for
     * @param minimumFrequency minimum frequency of term to be included in results
     * @return A sorted map of terms that have at least a frequency of <code>minimumFrequency</code> among the genes
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public Map<GeneOntologyTerm, Long> findRelatedTerms( Collection<Gene> genes, int minimumFrequency ) {
        Map<GeneOntologyTerm, Long> geneTermMap = findTermsAndFrequenciesByGenes( genes );

        // Remove all entries with a frequency less than minimumFrequency
        for ( Iterator<Map.Entry<GeneOntologyTerm, Long>> i = geneTermMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<GeneOntologyTerm, Long> goTerm = i.next();
            if ( goTerm.getValue() < minimumFrequency ) {
                i.remove();
            }
        }

        // Sort by frequency
        geneTermMap = sortByValue( geneTermMap );

        return geneTermMap;
    }

    /**
     * @param genes the collection of genes to find related terms for
     * @param minimumFrequency minimum frequency of term to be included in results
     * @param limit maximum genes associated with any returned terms
     * @return A sorted map of terms that have at least a frequency of <code>minimumFrequency</code> among the genes
     *         where each term has a maximum of <code>limit</code> genes associated with it
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    @Transactional
    @Override
    public Map<GeneOntologyTerm, Long> findRelatedTermsLimitedByTermSize( Collection<Gene> genes, int minimumFrequency,
            int limit ) {
        // This order of things is not optimal, but simpler...
        Map<GeneOntologyTerm, Long> geneTermMap = findTermsAndFrequenciesByGenesLimitedByTermSize( genes, limit );

        // Remove all entries with a frequency less than minimumFrequency
        for ( Iterator<Map.Entry<GeneOntologyTerm, Long>> i = geneTermMap.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<GeneOntologyTerm, Long> goTerm = i.next();
            if ( goTerm.getValue() < minimumFrequency ) {
                i.remove();
            }
        }

        // Sort by frequency
        geneTermMap = sortByValue( geneTermMap );

        return geneTermMap;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Map sortByValue( Map unsortMap ) {
        List list = new LinkedList( unsortMap.entrySet() );

        Collections.sort( list, new Comparator() {
            @Override
            public int compare( Object o1, Object o2 ) {
                return ( ( Comparable ) ( ( Map.Entry ) ( o1 ) ).getValue() ).compareTo( ( ( Map.Entry ) ( o2 ) )
                        .getValue() );
            }
        } );

        Collections.reverse( list );

        Map sortedMap = new LinkedHashMap();
        for ( Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = ( Map.Entry ) it.next();
            sortedMap.put( entry.getKey(), entry.getValue() );
        }
        return sortedMap;
    }

    @Transactional
    @Override
    public Collection<GeneAnnotation> loadAll() {
        return geneAnnotationDao.loadAll();
    }

    @Override
    @Transactional
    public void updateGeneAnnotationTable( String filePath ) {
        geneAnnotationDao.updateGeneAnnotationTable( filePath );
    }

    @Override
    @Transactional
    public void truncateGeneAnnotationTable() {
        geneAnnotationDao.truncateGeneAnnotationTable();
    }

    @Override
    @Transactional
    public List<Object[]> calculateDirectSizes() {
        return geneAnnotationDao.calculateDirectSizes();
    }

    @Deprecated
    @Override
    public Collection<GeneOntologyTerm> annotationToGeneOntologyId( Collection<GeneAnnotation> geneAnnotations ) {
        Collection<GeneOntologyTerm> results = new HashSet<GeneOntologyTerm>();

        for ( GeneAnnotation ga : geneAnnotations ) {
            results.add( new GeneOntologyTerm( ga.getGeneOntologyId(), ga.getGeneOntologyTerm() ) );
        }

        return results;
    }

    @Override
    public Collection<Gene> annotationToGene( Collection<GeneAnnotation> geneAnnotations ) {
        Collection<Gene> results = new HashSet<Gene>();

        for ( GeneAnnotation ga : geneAnnotations ) {
            results.add( ga.getGene() );
        }

        return results;
    }

}
