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

import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.larq.IndexLARQ;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.cache.GeneOntologyTermCache;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.util.Settings;
import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyClassRestriction;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologyIndexer;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.basecode.ontology.search.SearchIndex;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 * ubic.gemma.ontology.providers.GeneOntologyServiceImpl
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service("geneOntologyService")
public class GeneOntologyServiceImpl implements GeneOntologyService {

    public static enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    private static final String ALL_ROOT = BASE_GO_URI + "ALL";

    private static boolean enabled = true;

    @Autowired
    private GeneOntologyTermCache geneOntologyTermCache;

    @Autowired
    private TaxonService taxonService;

    /*
     * FIXME don't hardcode this.
     */
    private final static String GO_URL = "http://purl.obolibrary.org/obo/go.owl";
    // private final static String GO_URL = "http://purl.obolibrary.org/obo/go-basic.obo";

    private final static boolean LOAD_BY_DEFAULT = true;

    private static final String LOAD_GENE_ONTOLOGY_OPTION = "load.geneOntology";

    private static Log log = LogFactory.getLog( GeneOntologyServiceImpl.class );

    private static final String PART_OF_URI = "http://purl.obolibrary.org/obo/BFO_0000050";
    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );

    // cache
    private static Map<String, GOAspect> term2Aspect = new HashMap<>();

    // cache
    private static Map<String, OntologyTerm> uri2Term = new HashMap<>();

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        if ( term == null ) return null;
        String uri = term.getUri();
        return asRegularGoId( uri );
    }

    public static String asRegularGoId( String uri ) {
        return uri.replaceAll( ".*?/", "" ).replace( "_", ":" );
    }

    /**
     * @param goId e.g. GO:0001312
     * @return null if not found
     */
    public static OntologyTerm getTermForId( String goId ) {
        if ( uri2Term == null ) return null;

        if ( !uri2Term.containsKey( toUri( goId ) ) ) {
            log.warn( "GOID " + goId + " not recognized?" );
        }

        return uri2Term.get( toUri( goId ) );
    }

    /**
     * @param uri
     * @return null if not found
     */
    public static OntologyTerm getTermForURI( String uri ) {
        if ( uri2Term == null || !uri2Term.containsKey( uri ) ) return null;
        return uri2Term.get( uri );
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Turn an id like GO:0038128 into a URI.
     * 
     * @param goId
     * @return
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return BASE_GO_URI + uriTerm;
    }

    /**
     * Cache of go term -> child terms
     */
    private Map<String, Collection<OntologyTerm>> childrenCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    @Autowired
    private GeneAnnotationService geneAnnotationService;

    @Autowired
    private GeneService geneService;

    /**
     * Cache of gene -> go terms.
     */
    private Map<Gene, Collection<OntologyTerm>> goTerms = new HashMap<Gene, Collection<OntologyTerm>>();

    private Map<OntologyTerm, Long> goSizeCache = new HashMap<OntologyTerm, Long>();

    private static Map<OntologyTerm, Map<Long, Long>> goSizeByTaxonCache = new HashMap<OntologyTerm, Map<Long, Long>>();

    private Collection<SearchIndex> indices = new HashSet<>();

    private OntModel model;

    /**
     * Cache of go term -> parent terms
     */
    private Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() {
        this.init( false );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<OntologyTerm, Long> calculateGoTermFrequency( Collection<Gene> genes, Long taxonId,
            int minimumFrequency, int minimumTermSize, int maximumTermSize ) {
        Map<OntologyTerm, Long> frequencyMap = new HashMap<OntologyTerm, Long>();

        for ( Gene g : genes ) {
            // Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();
            Collection<OntologyTerm> directTerms = getGOTerms( g, false, null, true );

            // add all child terms
            // for ( OntologyTerm term : directTerms ) {
            // terms.addAll( getAllChildren( term ) );
            // }

            for ( OntologyTerm term : directTerms ) {
                if ( frequencyMap.containsKey( term ) ) {
                    frequencyMap.put( term, frequencyMap.get( term ) + 1 );
                } else {
                    frequencyMap.put( term, 1L );
                }
            }

        }

        // Limit by minimum frequency
        if ( minimumFrequency > 0 ) {
            for ( Iterator<Map.Entry<OntologyTerm, Long>> i = frequencyMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<OntologyTerm, Long> termEntry = i.next();
                if ( termEntry.getValue() < minimumFrequency ) {
                    i.remove();
                }
            }
        }

        // Limit by maximum gene pool size of go terms
        if ( maximumTermSize > 0 && minimumTermSize > 0 ) {
            for ( Iterator<Map.Entry<OntologyTerm, Long>> i = frequencyMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<OntologyTerm, Long> termEntry = i.next();
                OntologyTerm term = termEntry.getKey();
                // Limit to just a taxon?
                Long count = getGeneSize( term );
                if ( count > maximumTermSize || count < minimumTermSize ) {
                    i.remove();
                }
            }
        }

        // Reduce GO TERM redundancies
        frequencyMap = reduceRedundancies( frequencyMap );

        frequencyMap = sortByValue( frequencyMap );

        return frequencyMap;
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

    private Map<OntologyTerm, Long> reduceRedundancies( Map<OntologyTerm, Long> frequencyMap ) {
        // Reduce GO TERM redundancies
        Collection<OntologyTerm> markedForDeletion = new HashSet<OntologyTerm>();
        Map<OntologyTerm, Long> fmap = new HashMap<OntologyTerm, Long>( frequencyMap );
        for ( Map.Entry<OntologyTerm, Long> entry : fmap.entrySet() ) {
            OntologyTerm term = entry.getKey();
            Long frequency = entry.getValue();

            if ( markedForDeletion.contains( term ) ) {
                // Skip because a previous step would have taken care of this terms parents anyways
                continue;
            }

            Collection<OntologyTerm> parents = getAllParents( term, false );

            for ( OntologyTerm parent : parents ) {
                if ( fmap.containsKey( parent ) ) {
                    if ( fmap.get( parent ) > frequency ) {
                        // keep parent
                        markedForDeletion.add( term );
                        break;
                    } else {
                        // keep child
                        markedForDeletion.add( parent );
                    }
                }
            }

        }

        // Delete those marked for deletion
        for ( OntologyTerm redundantTerm : markedForDeletion ) {
            fmap.remove( redundantTerm );
        }

        return fmap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#calculateGoTermOverlap(ubic.gemma.model.genome.Gene,
     * java.util.Collection)
     */
    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        if ( queryGene == null ) return null;
        if ( geneIds.size() == 0 ) return overlap;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        overlap.put( queryGene.getId(), queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.findByMultipleIds( geneIds );

        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            if ( queryGeneTerms.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            Collection<OntologyTerm> comparisonOntos = getGOTerms( gene );

            if ( comparisonOntos == null || comparisonOntos.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            overlap.put( gene.getId(), computeOverlap( queryGeneTerms, comparisonOntos ) );
        }

        return overlap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#calculateGoTermOverlap(ubic.gemma.model.genome.Gene,
     * ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) {

        if ( queryGene1 == null || queryGene2 == null ) return null;

        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        return computeOverlap( queryGeneTerms1, queryGeneTerms2 );
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds ) {
        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        if ( queryGene == null ) return null;
        if ( geneIds.size() == 0 ) return overlap;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        overlap.put( queryGene, queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.findByMultipleIds( geneIds );

        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            if ( queryGeneTerms.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            Collection<OntologyTerm> comparisonOntos = getGOTerms( gene );

            if ( comparisonOntos == null || comparisonOntos.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            overlap.put( gene.getId(), computeOverlap( queryGeneTerms, comparisonOntos ) );
        }

        return overlap;
    }

    @Override
    public Long computeOverlapFrequency( String id, Collection<Gene> genes ) {
        OntologyTerm t = getTermForId( id );
        Long frequency = 0L;
        for ( Gene g : genes ) {
            Collection<OntologyTerm> directTerms = getGOTerms( g, false, null, false );

            for ( OntologyTerm term : directTerms ) {
                if ( term.equals( t ) ) {
                    frequency++;
                    // Can break because a gene cannot (and shouldn't) have duplicate terms
                    break;
                }
            }

        }
        return frequency;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#computeOverlap(java.util.Collection, java.util.Collection)
     */
    @Override
    public Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos ) {
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>( masterOntos );
        overlapTerms.retainAll( comparisonOntos );

        return overlapTerms;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#findTerm(java.lang.String)
     */
    @Override
    public Collection<OntologyTerm> findTerm( String queryString ) {

        if ( !isReady() ) return new HashSet<OntologyTerm>();

        if ( log.isDebugEnabled() ) log.debug( "Searching Gene Ontology for '" + queryString + "'" );

        // make sure we are all-inclusive
        queryString = queryString.trim();
        queryString = queryString.replaceAll( "\\s+", " AND " );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<OntologyResource> rawMatches = new HashSet<>();
        queryString = "a";
        for ( SearchIndex index : this.indices ) {
            rawMatches.addAll( OntologySearch.matchIndividuals( model, index, queryString ) );
        }
        if ( timer.getTime() > 100 ) {
            log.info( "Find " + rawMatches.size() + " raw go terms from " + queryString + ": " + timer.getTime()
                    + " ms" );
        }
        timer.reset();
        timer.start();
        log.info( rawMatches.size() );
        /*
         * Required to make sure the descriptions are filled in.
         */
        Collection<OntologyTerm> matches = new HashSet<>();
        for ( OntologyResource r : rawMatches ) {
            if ( StringUtils.isBlank( r.getUri() ) ) continue;
            OntologyTerm termForURI = GeneOntologyServiceImpl.getTermForURI( r.getUri() );
            if ( termForURI == null ) {
                log.warn( "No term for : " + r );
                continue;
            }
            matches.add( termForURI );
        }

        if ( timer.getTime() > 100 ) {
            log.info( "Convert " + rawMatches.size() + " raw go terms to terms: " + timer.getTime() + " ms" );
        }

        return matches;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllChildren(ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry ) {
        return getAllChildren( entry, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllChildren(ubic.basecode.ontology.model.OntologyTerm,
     * boolean)
     */
    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf ) {
        return getDescendants( entry, includePartOf );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllGOTermIds()
     */
    @Override
    public Collection<String> getAllGOTermIds() {
        Collection<String> goTermIds = uri2Term.keySet();
        return goTermIds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllParents(java.util.Collection)
     */
    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return getAllParents( entries, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllParents(java.util.Collection, boolean)
     */
    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        if ( entries == null ) return null;
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : entries ) {
            result.addAll( getAncestors( entry, includePartOf ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllParents(ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return getAllParents( entry, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getAllParents(ubic.basecode.ontology.model.OntologyTerm,
     * boolean)
     */
    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null ) return new HashSet<OntologyTerm>();
        return getAncestors( entry, includePartOf );
    }

    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry ) {
        return getChildren( entry, false );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getChildren(ubic.basecode.ontology.model.OntologyTerm,
     * boolean)
     */
    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null ) return null;
        if ( log.isDebugEnabled() ) log.debug( "Getting children of " + entry );
        Collection<OntologyTerm> terms = entry.getChildren( true );

        if ( includePartOf ) terms.addAll( getPartsOf( entry ) );

        return terms;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getGenes(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        return getGenes( goId, taxon.getId() );
    }

    @Override
    public Collection<Gene> getGenes( String goId, Long taxonId ) {
        OntologyTerm t = getTermForId( goId );
        return getGenes( t, taxonId );
    }

    @Override
    public Collection<Gene> getGenes( OntologyTerm t, Long taxonId ) {
        if ( t == null ) return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Collection<Gene> results = new HashSet<Gene>();
        Collection<GeneAnnotation> ga = geneAnnotationService
                .findByGeneOntologyIdAndTaxon( asRegularGoId( t ), taxonId );
        if ( ga != null ) {
            results = geneAnnotationService.annotationToGene( ga );
        }

        for ( OntologyTerm term : terms ) {
            ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( asRegularGoId( term ), taxonId );
            if ( ga != null ) {
                results.addAll( geneAnnotationService.annotationToGene( ga ) );
            }
        }
        return results;
    }

    @Override
    public Long getGeneSize( OntologyTerm t ) {
        if ( t == null ) return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Long size = getDirectGeneSize( t );
        for ( OntologyTerm term : terms ) {
            size += getDirectGeneSize( term );
        }

        return size;
    }

    @Override
    public Long getGeneSize( String id ) {
        return getGeneSize( getTermForId( id ) );
    }

    @Override
    public Long getDirectGeneSize( OntologyTerm t ) {
        if ( t == null ) return null;
        Long cachedSize = null;
        Map<Long, Long> cache = goSizeByTaxonCache.get( t );

        // Since we pre-compute direct sizes, if the cache is not found it means
        // there are no genes directly annotated with this term
        cachedSize = 0L;
        if ( cache != null ) {
            for ( Long taxonSize : cache.values() ) {
                cachedSize += taxonSize;
            }
        }

        return cachedSize;
    }

    @Override
    public Long getGeneSizeInTaxon( OntologyTerm t, Long taxonId ) {
        if ( t == null ) return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Long size = getDirectGeneSizeInTaxon( t, taxonId );
        for ( OntologyTerm term : terms ) {
            size += getDirectGeneSizeInTaxon( term, taxonId );
        }

        return size;
    }

    @Override
    public Long getGeneSizeInTaxon( String id, Long taxonId ) {
        return getGeneSizeInTaxon( getTermForId( id ), taxonId );
    }

    @Override
    public Long getDirectGeneSizeInTaxon( OntologyTerm t, Long taxonId ) {
        if ( t == null ) return null;

        Map<Long, Long> cache = goSizeByTaxonCache.get( t );

        // Since we pre-compute direct sizes, if the cache is not found it means
        // there are no genes directly annotated with this term
        Long cachedSize = 0L;
        if ( cache != null ) {
            cachedSize = cache.get( taxonId );

            if ( cachedSize == null ) {
                cachedSize = 0L;
            }
        }
        // String id = asRegularGoId( t );
        // if ( goSizeByTaxonCache.get( t ) == null ) {
        // goSizeByTaxonCache.put( t, new HashMap<Long, Long>() );
        // }
        // Long cachedSize = goSizeByTaxonCache.get( t ).get( taxonId );
        // if ( cachedSize == null ) {
        // cachedSize = geneAnnotationService.countGenesForGeneOntologyIdAndTaxon( id, taxonId );
        // goSizeByTaxonCache.get( t ).put( taxonId, cachedSize );
        // }

        return cachedSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getChildren(ubic.basecode.ontology.model.OntologyTerm)
     */

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getGOTerms(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return getGOTerms( gene, true, null, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getGOTerms(ubic.gemma.model.genome.Gene, boolean)
     */
    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        return getGOTerms( gene, includePartOf, null, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getGOTerms(ubic.gemma.model.genome.Gene, boolean,
     * ubic.gemma.ontology.providers.GeneOntologyServiceImpl.GOAspect)
     */
    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect ) {
        return getGOTerms( gene, includePartOf, null, true );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect,
            boolean includeParents ) {
        if ( !isReady() ) return new HashSet<OntologyTerm>();
        Collection<OntologyTerm> cachedTerms = goTerms.get( gene );
        if ( log.isTraceEnabled() && cachedTerms != null ) {
            logIds( "found cached GO terms for " + gene.getOfficialSymbol(), goTerms.get( gene ) );
        }

        if ( cachedTerms == null ) {
            Collection<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();

            // Collection<VocabCharacteristic> annotations = gene2GOAssociationService.findByGene( gene );
            Collection<GeneAnnotation> annotations = geneAnnotationService.findByGene( gene );
            for ( GeneAnnotation ga : annotations ) {
                if ( !uri2Term.containsKey( toUri( ga.getGeneOntologyId() ) ) ) {
                    log.warn( "Term " + toUri( ga.getGeneOntologyId() ) + " not found in term list cant add to results" );
                    continue;
                }
                allGOTermSet.add( uri2Term.get( toUri( ga.getGeneOntologyId() ) ) );
            }

            if ( includeParents ) {
                allGOTermSet.addAll( getAllParents( allGOTermSet, includePartOf ) );
            }

            cachedTerms = Collections.unmodifiableCollection( allGOTermSet );
            if ( log.isTraceEnabled() ) logIds( "caching GO terms for " + gene.getOfficialSymbol(), allGOTermSet );
            goTerms.put( gene, cachedTerms );
        }

        if ( goAspect != null ) {

            Collection<OntologyTerm> finalTerms = new HashSet<OntologyTerm>();

            for ( OntologyTerm ontologyTerm : cachedTerms ) {
                if ( getTermAspect( ontologyTerm ).equals( goAspect ) ) {
                    finalTerms.add( ontologyTerm );
                }
            }

            return finalTerms;
        }

        return cachedTerms;
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Long geneId ) {
        return getGOTerms( geneId, true, null );
    }

    /**
     * FIXME it might be better to avoid the fetch.
     * 
     * @param gene
     * @param includePartOf
     * @param goAspect
     * @return
     */
    public Collection<OntologyTerm> getGOTerms( Long gene, boolean includePartOf, GOAspect goAspect ) {
        return this.getGOTerms( geneService.findById( gene ), includePartOf, goAspect, true );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getParents(ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        return getParents( entry, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getParents(ubic.basecode.ontology.model.OntologyTerm,
     * boolean)
     */
    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( OntologyTerm term : parents ) {
            // The isRoot() returns true for the MolecularFunction, BiologicalProcess, CellularComponent
            if ( term.isRoot() ) continue;
            if ( term.getUri().equalsIgnoreCase( ALL_ROOT ) ) continue;

            if ( term instanceof OntologyClassRestriction ) {
                // log.info( "Skipping " + term );
                // OntologyProperty restrictionOn = ( ( OntologyClassRestriction ) term ).getRestrictionOn();
                // if ( restrictionOn.getLabel().equals( "part_of" ) ) {
                // OntologyTerm restrictedTo = ( ( OntologyClassRestriction ) term ).getRestrictedTo();
                // results.add( restrictedTo );
                // }
                // don't keep it. - for example, "ends_during" RO_0002093
            } else {
                // log.info( "Adding " + term );
                results.add( term );
            }
        }

        if ( includePartOf ) results.addAll( getIsPartOf( entry ) );

        return results;
    }

    /**
     * @param goId
     * @return
     */
    @Override
    public GOAspect getTermAspect( String goId ) {
        OntologyTerm term = getTermForId( goId );
        if ( term == null ) return null;
        return getTermAspect( term );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getTermDefinition(java.lang.String)
     */
    @Override
    public String getTermDefinition( String goId ) {
        OntologyTerm t = getTermForId( goId );
        return getTermDefinition( t );
    }

    @Override
    public String getTermDefinition( OntologyTerm t ) {
        assert t != null;
        Collection<AnnotationProperty> annotations = t.getAnnotations();
        for ( AnnotationProperty annot : annotations ) {
            // log.info( annot.getProperty() );
            if ( annot.getProperty().equals( "hasDefinition" ) || annot.getProperty().equals( "definition" ) ) {
                return annot.getContents();
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#getTermName(java.lang.String)
     */
    @Override
    public String getTermName( String goId ) {

        OntologyTerm t = getTermForId( goId );
        if ( t == null ) return "[Not available]"; // not ready yet?
        return t.getTerm();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#init(boolean)
     */
    @Override
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        boolean loadOntology = Settings.getBoolean( LOAD_GENE_ONTOLOGY_OPTION, LOAD_BY_DEFAULT );

        if ( !force && !loadOntology ) {
            log.info( "Loading Gene Ontology is disabled (force=" + force + ", " + LOAD_GENE_ONTOLOGY_OPTION + "="
                    + loadOntology + ")" );
            enabled = false;
            return;
        }

        initializeGeneOntology();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isAChildOf(ubic.basecode.ontology.model.OntologyTerm,
     * ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild ) {
        return isAParentOf( potentialChild, parent );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isAParentOf(ubic.basecode.ontology.model.OntologyTerm,
     * ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent ) {
        if ( potentialParent.isRoot() ) return true; // well....
        Collection<OntologyTerm> parents = getAllParents( child );
        return parents.contains( potentialParent );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isAValidGOId(java.lang.String)
     */
    @Override
    public Boolean isAValidGOId( String goId ) {
        if ( !this.isReady() ) {
            throw new UnsupportedOperationException( "Gene ontology isn't ready so cannot check validity of IDs" );
        }
        if ( uri2Term.containsKey( toUri( goId ) ) ) return true;
        return uri2Term.containsKey( toUri( goId.replaceFirst( "_", ":" ) ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.ontology.providers.GeneOntologyService#isBiologicalProcess(ubic.basecode.ontology.model.OntologyTerm)
     */
    @Override
    public boolean isBiologicalProcess( OntologyTerm term ) {

        GOAspect nameSpace = getTermAspect( term );
        if ( nameSpace == null ) {
            log.warn( "No namespace for " + term + ", assuming not Biological Process" );
            return false;
        }

        return nameSpace.equals( GOAspect.BIOLOGICAL_PROCESS );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isGeneOntologyLoaded()
     */
    @Override
    public synchronized boolean isGeneOntologyLoaded() {

        return ready.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isReady()
     */
    @Override
    public boolean isReady() {
        return ready.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#isRunning()
     */
    @Override
    public boolean isRunning() {
        return running.get();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#listTerms()
     */
    @Override
    public Collection<OntologyTerm> listTerms() {
        return uri2Term.values();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.providers.GeneOntologyService#loadTermsInNameSpace(java.io.InputStream)
     */
    @Override
    public void loadTermsInNameSpace( InputStream is ) {
        this.model = OntologyLoader.loadMemoryModel( is, null, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( null, model );
        this.indices.add( OntologyIndexer.indexOntology( "GeneOntology", model ) );
        uri2Term.clear();
        addTerms( terms );
        ready.set( true );
    }

    @Override
    public void shutDown() {
        if ( this.isReady() ) {
            try {
                this.goTerms.clear();
                this.childrenCache.clear();
                this.goSizeCache.clear();
                this.parentsCache.clear();
                term2Aspect.clear();
                for ( IndexLARQ l : indices ) {
                    l.close();
                }

                this.model.close();
                this.model = null;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            } finally {
                ready.set( false );
                running.set( false );
            }

        }

    }

    /**
     * 
     */
    protected synchronized void forceLoadOntology() {
        initializeGeneOntology();
    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) {
        this.model = OntologyLoader.loadMemoryModel( url, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, model );
        this.indices.add( OntologyIndexer.indexOntology( url.replaceFirst( ".*/", "" ).replace( ".owl", "" ), model ) );
        addTerms( terms );
    }

    /**
     * @param newTerms
     */
    private void addTerms( Collection<OntologyResource> newTerms ) {

        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null ) continue;
            if ( term instanceof OntologyTerm ) {
                OntologyTerm ontTerm = ( OntologyTerm ) term;
                uri2Term.put( term.getUri(), ontTerm );
                for ( String alternativeID : ontTerm.getAlternativeIds() ) {
                    log.debug( toUri( alternativeID ) );
                    uri2Term.put( toUri( alternativeID ), ontTerm );
                }
            }
        }
    }

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    private synchronized Collection<OntologyTerm> getAncestors( OntologyTerm entry, boolean includePartOf ) {

        if ( !isReady() ) return new HashSet<OntologyTerm>();

        if ( entry == null ) {
            return new HashSet<OntologyTerm>();
        }

        Collection<OntologyTerm> ancestors = parentsCache.get( entry.getUri() );
        if ( ancestors == null ) {
            ancestors = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> parents = getParents( entry, includePartOf );
            if ( parents != null ) {
                for ( OntologyTerm parent : parents ) {
                    ancestors.add( parent );
                    ancestors.addAll( getAncestors( parent, includePartOf ) );
                }
            }

            ancestors = Collections.unmodifiableCollection( ancestors );
            parentsCache.put( entry.getUri(), ancestors );
        }
        return new HashSet<OntologyTerm>( ancestors );
    }

    /**
     * @param entry
     * @return Given an ontology term recursivly determines all the children and adds them to a cache (same as
     *         getAllParents but the recusive code is a little cleaner and doesn't use and accumulator)
     */
    private synchronized Collection<OntologyTerm> getDescendants( OntologyTerm entry, boolean includePartOf ) {

        if ( !isReady() ) return new HashSet<OntologyTerm>();

        Collection<OntologyTerm> descendants = childrenCache.get( entry.getUri() );
        if ( descendants == null ) {
            descendants = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> children = getChildren( entry, includePartOf );
            if ( children != null ) {
                for ( OntologyTerm child : children ) {
                    descendants.add( child );
                    descendants.addAll( getDescendants( child, includePartOf ) );
                }
            }

            descendants = Collections.unmodifiableCollection( descendants );
            childrenCache.put( entry.getUri(), descendants );
        }
        return new HashSet<OntologyTerm>( descendants );

    }

    /**
     * Return terms to which the given term has a part_of relation (it is "part_of" them).
     * 
     * @param entry
     * @return
     */
    private Collection<OntologyTerm> getIsPartOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {  <" + u + ">  <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty>  <" + PART_OF_URI + "> . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> ?x . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                if ( x.isAnon() ) continue; // some reasoners will return these.
                String uri = x.getURI();
                if ( log.isDebugEnabled() ) log.debug( entry + " is part of " + uri2Term.get( uri ) );
                r.add( uri2Term.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * Return terms which have "part_of" relation with the given term (they are "part_of" the given term).
     * 
     * @param entry
     * @return
     */
    private Collection<OntologyTerm> getPartsOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {" + "?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty> <" + PART_OF_URI + ">  . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> <" + u + "> . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                String uri = x.getURI();
                if ( x.isAnon() ) continue; // some reasoners will return these.
                if ( log.isDebugEnabled() ) log.debug( uri2Term.get( uri ) + " is part of " + entry );
                r.add( uri2Term.get( uri ) );
            }
        } catch ( Exception e ) {
            log.error( e, e );
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * @param term
     * @return
     */
    private GOAspect getTermAspect( OntologyTerm term ) {

        if ( !isReady() ) return null;

        assert term != null;
        String goid = term.getTerm();

        if ( term2Aspect.containsKey( goid ) ) {
            return term2Aspect.get( goid );
        }

        String nameSpace = null;
        for ( AnnotationProperty annot : term.getAnnotations() ) {
            /*
             * Why they changed this, I can't say. It used to be hasOBONamespace but now comes through as
             * has_obo_namespace.
             */
            if ( annot.getProperty().equals( "hasOBONamespace" ) || annot.getProperty().equals( "has_obo_namespace" ) ) {
                nameSpace = annot.getContents();
                break;
            }
        }

        if ( nameSpace == null ) {
            log.warn( "aspect could not be determined for: " + term );
            return null;
        }

        GOAspect aspect = GOAspect.valueOf( nameSpace.toUpperCase() );
        term2Aspect.put( goid, aspect );

        return aspect;
    }

    /**
     * 
     */
    private synchronized void initializeGeneOntology() {
        if ( running.get() ) return;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                running.set( true );
                uri2Term = new HashMap<String, OntologyTerm>();
                log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {
                    loadTermsInNameSpace( GO_URL );
                    precomputeSizes();
                    log.info( "GOSize cache size: " + goSizeByTaxonCache.size() );
                    log.info( "Gene Ontology loaded, total of " + uri2Term.size() + " items in " + loadTime.getTime()
                            / 1000 + "s" );
                    ready.set( true );
                    running.set( false );

                    loadTime.reset();
                    loadTime.start();
                    long cacheSize = updateEhCache();
                    log.info( "Gene Ontology ehCache, total of " + cacheSize + " items in " + loadTime.getTime() / 1000
                            + "s" );

                    log.info( "Done loading GO" );
                    loadTime.stop();
                } catch ( Throwable e ) {
                    if ( log != null ) log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        loadThread.start();

    }

    private void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuffer buf = new StringBuffer( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( GeneOntologyServiceImpl.asRegularGoId( i.next() ) );
            if ( i.hasNext() ) buf.append( ", " );
        }
        buf.append( " ]" );
        log.trace( buf.toString() );
    }

    private void precomputeSizes() {
        List<Object[]> list = geneAnnotationService.calculateDirectSizes();
        for ( Object[] entity : list ) {
            String id = ( String ) entity[0];

            Long taxonId = ( Long ) entity[1];
            Long count = ( Long ) entity[2];
            OntologyTerm term = getTermForId( id );

            if ( goSizeByTaxonCache.get( term ) == null ) {
                goSizeByTaxonCache.put( term, new HashMap<Long, Long>() );
            }
            goSizeByTaxonCache.get( term ).put( taxonId, count );

        }

    }

    private long updateEhCache() {
        long cacheSize = -1;
        Collection<OntologyTerm> terms = listTerms();
        // prepare terms
        Collection<GeneOntologyTerm> goTerms = new HashSet<GeneOntologyTerm>();

        for ( OntologyTerm term : terms ) {
            GeneOntologyTerm goTerm = new GeneOntologyTerm( term );
            // goTerm.setFrequency( 0L );
            // goTerm.setSize( geneOntologyService.getGeneSize( term ) );
            goTerm.setDefinition( getTermDefinition( term ) );
            goTerm.setAspect( getTermAspect( term ) );
            goTerms.add( goTerm );

        }
        geneOntologyTermCache.putAll( goTerms );

        cacheSize = geneOntologyTermCache.size();
        log.info( "Current size of Cache: " + cacheSize );

        return cacheSize;

    }

    @Override
    public Collection<GeneOntologyTerm> fetchByQuery( String queryString ) {
        return geneOntologyTermCache.fetchByQuery( queryString );
    }

    @Override
    public Collection<GeneOntologyTerm> deserializeGOTerms( String[] GOJSON ) {
        Collection<GeneOntologyTerm> results = new HashSet<GeneOntologyTerm>();
        for ( int i = 0; i < GOJSON.length; i++ ) {
            JSONObject json = new JSONObject( GOJSON[i] );
            if ( !json.has( "geneOntologyId" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            String geneOntologyId = json.getString( "geneOntologyId" );

            if ( StringUtils.isEmpty( geneOntologyId ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }

            GeneOntologyTerm term = geneOntologyTermCache.fetchById( geneOntologyId );
            if ( !( term == null ) ) {
                results.add( term );
            } else {
                // it doesn't exist in cache
                log.warn( "Cannot deserialize GO Term: " + geneOntologyId );
            }
        }

        return results;
    }

    @Override
    public Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Long taxonId ) {
        Collection<Gene> results = new HashSet<Gene>();

        for ( GeneOntologyTerm term : goTerms ) {
            results.addAll( getGenes( term.getGeneOntologyId(), taxonId ) );
        }

        return results;

    }

    @Override
    public JSONArray toJSON( Collection<GeneOntologyTerm> goTerms ) {
        Collection<JSONObject> jsonSet = new HashSet<JSONObject>();
        for ( GeneOntologyTerm term : goTerms ) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put( "geneOntologyId", term.getGeneOntologyId() );
            jsonObj.put( "aspect", term.getAspect() );
            jsonObj.put( "geneOntologyTerm", term.getGeneOntologyTerm() );
            jsonObj.put( "definition", term.getDefinition() );
            jsonObj.put( "taxonId", term.getTaxonId() );

            jsonObj.put( "size", term.getSize() );
            jsonObj.put( "frequency", term.getFrequency() );
            jsonSet.add( jsonObj );
        }

        return new JSONArray( jsonSet );

    }

}
