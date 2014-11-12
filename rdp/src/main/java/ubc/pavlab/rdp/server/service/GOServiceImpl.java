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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.cache.GeneOntologyTermCache;
import ubc.pavlab.rdp.server.model.GOTerm;
import ubc.pavlab.rdp.server.model.GOTerm.Relationship;
import ubc.pavlab.rdp.server.model.GOTerm.Relationship.RelationshipType;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service("gOService")
public class GOServiceImpl implements GOService {

    public static enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    private static Log log = LogFactory.getLog( GOServiceImpl.class );

    private final static String GO_URL = "http://purl.obolibrary.org/obo/go.obo";

    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );

    private static Map<String, GOTerm> termMap = new HashMap<>();

    private static Map<GOTerm, Map<Long, Long>> termSizeByTaxonCache = new HashMap<GOTerm, Map<Long, Long>>();

    @Autowired
    private GeneOntologyTermCache geneOntologyTermCache;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneAnnotationService geneAnnotationService;

    @Autowired
    private GeneService geneService;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<GOTerm, Long> calculateGoTermFrequency( Collection<Gene> genes, Long taxonId, int minimumFrequency,
            int minimumTermSize, int maximumTermSize ) {
        Map<GOTerm, Long> frequencyMap = new HashMap<GOTerm, Long>();

        for ( Gene g : genes ) {
            // Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();
            Collection<GOTerm> directTerms = getGOTerms( g, true, true );

            // add all child terms
            // for ( OntologyTerm term : directTerms ) {
            // terms.addAll( getAllChildren( term ) );
            // }

            for ( GOTerm term : directTerms ) {
                if ( frequencyMap.containsKey( term ) ) {
                    frequencyMap.put( term, frequencyMap.get( term ) + 1 );
                } else {
                    frequencyMap.put( term, 1L );
                }
            }

        }

        // Limit by minimum frequency
        if ( minimumFrequency > 0 ) {
            for ( Iterator<Map.Entry<GOTerm, Long>> i = frequencyMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<GOTerm, Long> termEntry = i.next();
                if ( termEntry.getValue() < minimumFrequency ) {
                    i.remove();
                }
            }
        }

        // Limit by maximum gene pool size of go terms
        if ( maximumTermSize > 0 && minimumTermSize > 0 ) {
            for ( Iterator<Map.Entry<GOTerm, Long>> i = frequencyMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<GOTerm, Long> termEntry = i.next();
                GOTerm term = termEntry.getKey();
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

    private Map<GOTerm, Long> reduceRedundancies( Map<GOTerm, Long> frequencyMap ) {
        // Reduce GO TERM redundancies
        Collection<GOTerm> markedForDeletion = new HashSet<GOTerm>();
        Map<GOTerm, Long> fmap = new HashMap<GOTerm, Long>( frequencyMap );
        for ( Map.Entry<GOTerm, Long> entry : fmap.entrySet() ) {
            GOTerm term = entry.getKey();
            Long frequency = entry.getValue();

            if ( markedForDeletion.contains( term ) ) {
                // Skip because a previous step would have taken care of this terms parents anyways
                continue;
            }

            Collection<GOTerm> parents = getAncestors( term, true );

            for ( GOTerm parent : parents ) {
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
        for ( GOTerm redundantTerm : markedForDeletion ) {
            fmap.remove( redundantTerm );
        }

        return fmap;
    }

    @Override
    public Collection<GeneOntologyTerm> fetchByQuery( String queryString ) {
        return geneOntologyTermCache.fetchByQuery( queryString );
    }

    @Override
    public Collection<GOTerm> getChildren( GOTerm entry ) {
        return getChildren( entry, true );
    }

    @Override
    public Collection<GOTerm> getChildren( GOTerm entry, boolean includePartOf ) {
        Collection<Relationship> relationships = entry.getChildren();
        Collection<GOTerm> results = new HashSet<GOTerm>();

        for ( Relationship relationship : relationships ) {
            if ( includePartOf || relationship.getType().equals( RelationshipType.IS_A ) ) {
                results.add( termMap.get( relationship.getId() ) );
            }
        }

        return results;
    }

    @Override
    public Collection<GOTerm> getParents( GOTerm entry ) {
        return getParents( entry, true );
    }

    @Override
    public Collection<GOTerm> getParents( GOTerm entry, boolean includePartOf ) {
        Collection<Relationship> relationships = entry.getParents();
        Collection<GOTerm> results = new HashSet<GOTerm>();

        for ( Relationship relationship : relationships ) {
            if ( includePartOf || relationship.getType().equals( RelationshipType.IS_A ) ) {
                results.add( termMap.get( relationship.getId() ) );
            }
        }

        return results;
    }

    @Override
    public Collection<GOTerm> getDescendants( GOTerm entry ) {
        return getDescendants( entry, true );
    }

    @Override
    public Collection<GOTerm> getDescendants( GOTerm entry, boolean includePartOf ) {
        Collection<GOTerm> descendants = new HashSet<GOTerm>();

        Collection<GOTerm> children = getChildren( entry, includePartOf );
        for ( GOTerm child : children ) {
            descendants.add( child );
            descendants.addAll( getDescendants( child, includePartOf ) );
        }

        descendants = Collections.unmodifiableCollection( descendants );

        return new HashSet<GOTerm>( descendants );
    }

    @Override
    public Collection<GOTerm> getAncestors( GOTerm entry ) {
        return getAncestors( entry, true );
    }

    @Override
    public Collection<GOTerm> getAncestors( GOTerm entry, boolean includePartOf ) {
        Collection<GOTerm> ancestors = new HashSet<GOTerm>();

        Collection<GOTerm> parents = getParents( entry, includePartOf );
        for ( GOTerm parent : parents ) {
            ancestors.add( parent );
            ancestors.addAll( getAncestors( parent, includePartOf ) );
        }

        ancestors = Collections.unmodifiableCollection( ancestors );

        return new HashSet<GOTerm>( ancestors );
    }

    @Override
    public Collection<Gene> getGenes( String id, Long taxonId ) {
        return getGenes( termMap.get( id ), taxonId );
    }

    @Override
    public Collection<Gene> getGenes( GOTerm t, Long taxonId ) {
        if ( t == null ) return null;
        Collection<GOTerm> descendants = getDescendants( t );
        Collection<Gene> results = new HashSet<Gene>();
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( t.getId(), taxonId );
        if ( ga != null ) {
            results = geneAnnotationService.annotationToGene( ga );
        }

        for ( GOTerm term : descendants ) {
            ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( term.getId(), taxonId );
            if ( ga != null ) {
                results.addAll( geneAnnotationService.annotationToGene( ga ) );
            }
        }
        return results;
    }

    @Override
    public Collection<GOTerm> getGOTerms( Gene gene ) {
        return getGOTerms( gene, true, true );
    }

    @Override
    public Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        return getGOTerms( gene, includePartOf, true );
    }

    @Override
    public Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf, boolean propagateUpwards ) {

        Collection<GOTerm> allGOTermSet = new HashSet<GOTerm>();

        Collection<GeneAnnotation> annotations = geneAnnotationService.findByGene( gene );
        if ( annotations != null ) {
            for ( GeneAnnotation ga : annotations ) {
                if ( !termMap.containsKey( ga.getGeneOntologyId() ) ) {
                    log.warn( "Term " + ga.getGeneOntologyId() + " not found in term map cant add to results" );
                    continue;
                }

                GOTerm term = termMap.get( ga.getGeneOntologyId() );

                allGOTermSet.add( term );

                if ( propagateUpwards ) {
                    allGOTermSet.addAll( getAncestors( term, includePartOf ) );
                }
            }

            allGOTermSet = Collections.unmodifiableCollection( allGOTermSet );
        }

        return allGOTermSet;
    }

    @Override
    public Long getGeneSize( GOTerm t ) {
        if ( t == null ) return null;
        Collection<GOTerm> terms = getDescendants( t );
        Long size = getDirectGeneSize( t );
        for ( GOTerm term : terms ) {
            size += getDirectGeneSize( term );
        }

        return size;
    }

    @Override
    public Long getDirectGeneSize( GOTerm t ) {
        if ( t == null ) return null;
        Long cachedSize = null;
        Map<Long, Long> cache = termSizeByTaxonCache.get( t );

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
    public Long getGeneSizeInTaxon( String id, Long taxonId ) {
        return getGeneSizeInTaxon( termMap.get( id ), taxonId );
    }

    @Override
    public Long getGeneSizeInTaxon( GOTerm t, Long taxonId ) {
        if ( t == null ) return null;
        Collection<GOTerm> terms = getDescendants( t );
        Long size = getDirectGeneSizeInTaxon( t, taxonId );
        for ( GOTerm term : terms ) {
            size += getDirectGeneSizeInTaxon( term, taxonId );
        }

        return size;
    }

    @Override
    public Long getDirectGeneSizeInTaxon( GOTerm t, Long taxonId ) {
        if ( t == null ) return null;

        Map<Long, Long> cache = termSizeByTaxonCache.get( t );

        // Since we pre-compute direct sizes, if the cache is not found it means
        // there are no genes directly annotated with this term
        Long cachedSize = 0L;
        if ( cache != null ) {
            cachedSize = cache.get( taxonId );

            if ( cachedSize == null ) {
                cachedSize = 0L;
            }
        }

        return cachedSize;
    }

    @Override
    public Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Long taxonId ) {
        Collection<Gene> results = new HashSet<Gene>();

        for ( GeneOntologyTerm term : goTerms ) {
            results.addAll( getGenes( termMap.get( term.getGeneOntologyId() ), taxonId ) );
        }

        return results;
    }

    @Override
    public Long computeOverlapFrequency( String id, Collection<Gene> genes ) {
        GOTerm t = termMap.get( id );
        Long frequency = 0L;
        for ( Gene g : genes ) {
            Collection<GOTerm> directTerms = getGOTerms( g, true, true );

            for ( GOTerm term : directTerms ) {
                if ( term.equals( t ) ) {
                    frequency++;
                    // Can break because a gene cannot (and shouldn't) have duplicate terms
                    break;
                }
            }

        }
        return frequency;
    }

    @Override
    public void init() {
        if ( running.get() ) {
            log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        initializeGeneOntology();
    }

    private synchronized void initializeGeneOntology() {
        if ( running.get() ) return;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                running.set( true );
                termMap = new HashMap<String, GOTerm>();
                log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();

                try {
                    loadTermsInNameSpace( GO_URL );
                    precomputeSizes();
                    log.info( "GOSize cache size: " + termSizeByTaxonCache.size() );
                    log.info( "Gene Ontology loaded, total of " + termMap.size() + " items in " + loadTime.getTime()
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

    /**
     * @param url
     * @throws MalformedURLException
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) throws MalformedURLException, IOException {
        InputStream input = new URL( url ).openStream();
        GOParser gOParser = new GOParser( input );
        termMap = gOParser.getMap();
    }

    private void precomputeSizes() {
        List<Object[]> list = geneAnnotationService.calculateDirectSizes();
        Collection<String> nullIds = new HashSet<String>();
        for ( Object[] entity : list ) {
            String id = ( String ) entity[0];

            Long taxonId = ( Long ) entity[1];
            Long count = ( Long ) entity[2];
            GOTerm term = termMap.get( id );

            if ( term == null ) {
                nullIds.add( id );
                continue;
            }

            if ( termSizeByTaxonCache.get( term ) == null ) {
                termSizeByTaxonCache.put( term, new HashMap<Long, Long>() );
            }
            termSizeByTaxonCache.get( term ).put( taxonId, count );

        }

        if ( nullIds.size() > 0 ) {
            log.warn( "Could not find " + nullIds.size() + " terms in map " );
        }

    }

    private long updateEhCache() {
        long cacheSize = -1;
        Collection<GOTerm> terms = termMap.values();
        // prepare terms
        Collection<GeneOntologyTerm> goTerms = new HashSet<GeneOntologyTerm>();

        for ( GOTerm term : terms ) {
            GeneOntologyTerm goTerm = new GeneOntologyTerm( term );
            // goTerm.setFrequency( 0L );
            // goTerm.setSize( geneOntologyService.getGeneSize( term ) );
            goTerm.setDefinition( term.getDefinition() );
            goTerm.setAspect( GOAspect.valueOf( term.getAspect().toUpperCase() ) );
            goTerms.add( goTerm );

        }
        geneOntologyTermCache.putAll( goTerms );

        cacheSize = geneOntologyTermCache.size();
        log.info( "Current size of Cache: " + cacheSize );

        return cacheSize;

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
    public JSONArray toJSON( Collection<GeneOntologyTerm> goTerms ) {
        Collection<JSONObject> jsonList = new ArrayList<JSONObject>();
        for ( GeneOntologyTerm term : goTerms ) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put( "geneOntologyId", term.getGeneOntologyId() );
            jsonObj.put( "aspect", term.getAspect() );
            jsonObj.put( "geneOntologyTerm", term.getGeneOntologyTerm() );
            jsonObj.put( "definition", term.getDefinition() );
            jsonObj.put( "taxonId", term.getTaxonId() );

            jsonObj.put( "size", term.getSize() );
            jsonObj.put( "frequency", term.getFrequency() );
            jsonList.add( jsonObj );
        }

        return new JSONArray( jsonList );
    }

}
