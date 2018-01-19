package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneAnnotation;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.AggregateCount;
import ubc.pavlab.rdp.util.GOParser;
import ubc.pavlab.rdp.util.GOTerm;
import ubc.pavlab.rdp.util.GOTerm.Relationship;
import ubc.pavlab.rdp.util.GOTerm.Relationship.RelationshipType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("gOService")
public class GOServiceImpl implements GOService {

    private static Log log = LogFactory.getLog( GOServiceImpl.class );

    private final static String GO_URL = "http://purl.obolibrary.org/obo/go.obo";

    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );

    private static Map<String, GOTerm> termMap = new HashMap<>();

    private static Map<GOTerm, Collection<GOTerm>> descendantsCache = new HashMap<>();

    @Autowired
    private GeneAnnotationService geneAnnotationService;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<GOTerm, Long> calculateGoTermFrequency( Collection<Gene> genes, Long taxonId, int minimumFrequency,
                                                       int minimumTermSize, int maximumTermSize ) {
        Map<GOTerm, Long> frequencyMap = new HashMap<>();
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
        log.debug( "Calculate overlaps for each term with propagation done." );
        // Limit by minimum frequency
        if ( minimumFrequency > 0 ) {
            frequencyMap.entrySet().removeIf( termEntry -> termEntry.getValue() < minimumFrequency );
        }
        log.debug( "Limit by min freq done." );

        // Limit by maximum gene pool size of go terms
        if ( maximumTermSize > 0 && minimumTermSize > 0 ) {
            for ( Iterator<Map.Entry<GOTerm, Long>> i = frequencyMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<GOTerm, Long> termEntry = i.next();
                GOTerm term = termEntry.getKey();
                // Limit to just a taxon?
                Integer count = getGeneSize( term );
                if ( count > maximumTermSize || count < minimumTermSize ) {
                    i.remove();
                }
            }
        }
        log.debug( "Limit by size done." );

        // Reduce GO TERM redundancies
        frequencyMap = reduceRedundancies( frequencyMap );

        log.debug( "Reduce redundancy done." );

        frequencyMap = sortByValue( frequencyMap );
        log.debug( "Sort done." );

        return frequencyMap;
    }

    @SuppressWarnings("rawtypes")
    private Map sortByValue( Map unsortMap ) {
        return sortByValue( unsortMap, true );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map sortByValue( Map unsortMap, boolean reverse ) {
        List list = new LinkedList( unsortMap.entrySet() );

        list.sort( ( o1, o2 ) -> ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo( ((Map.Entry) (o2))
                .getValue() ) );

        if ( reverse ) {
            Collections.reverse( list );
        }

        Map sortedMap = new LinkedHashMap();
        for ( Object aList : list ) {
            Map.Entry entry = (Map.Entry) aList;
            sortedMap.put( entry.getKey(), entry.getValue() );
        }
        return sortedMap;
    }

    private Map<GOTerm, Long> reduceRedundancies( Map<GOTerm, Long> frequencyMap ) {
        // Reduce GO TERM redundancies
        Collection<GOTerm> markedForDeletion = new HashSet<>();
        Map<GOTerm, Long> fmap = new HashMap<>( frequencyMap );
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

    @SuppressWarnings("unchecked")
    @Override
    public Collection<GOTerm> search( String queryString ) {
        if ( queryString == null ) return new ArrayList<>();

        // ArrayList<GOTerm> results = new ArrayList<GOTerm>();
        Map<GOTerm, Long> results = new HashMap<>();
        // log.info( "search: " + queryString );
        for ( GOTerm term : termMap.values() ) {
            if ( queryString.equals( term.getId() ) || ("GO:" + queryString).equals( term.getId() ) ) {
                results.put( term, 1L );
                continue;
            }

            String pattern = "(?i:.*" + Pattern.quote( queryString ) + ".*)";
            // Pattern r = Pattern.compile(pattern);
            String m = term.getTerm();
            // Matcher m = r.matcher( term.getTerm() );
            if ( m.matches( pattern ) ) {
                results.put( term, 2L );
                continue;
            }

            m = term.getDefinition();
            // m = r.matcher( term.getDefinition() );
            if ( m.matches( pattern ) ) {
                results.put( term, 3L );
                continue;
            }

            String[] queryList = queryString.split( " " );
            for ( String q : queryList ) {
                pattern = "(?i:.*" + Pattern.quote( q ) + ".*)";
                // r = Pattern.compile(pattern);
                m = term.getTerm();
                // m = r.matcher( term.getTerm() );

                if ( m.matches( pattern ) ) {
                    results.put( term, 4L );
                    break;
                }

            }

            if ( results.containsKey( term ) ) continue;

            for ( String q : queryList ) {
                pattern = "(?i:.*" + Pattern.quote( q ) + ".*)";
                // r = Pattern.compile(pattern);
                // m = r.matcher( term.getDefinition() );
                m = term.getDefinition();
                if ( m.matches( pattern ) ) {
                    results.put( term, 5L );
                    break;
                }

            }

        }

        // log.info( "search result size " + results.size() );

        // Now we have a set of terms with how well they match
        results = sortByValue( results, false );

        return results.keySet();

    }

    @Override
    public Collection<GOTerm> getChildren( GOTerm entry ) {
        return getChildren( entry, true );
    }

    @Override
    public Collection<GOTerm> getChildren( GOTerm entry, boolean includePartOf ) {
        Collection<Relationship> relationships = entry.getChildren();
        Collection<GOTerm> results = new HashSet<>();

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
        Collection<GOTerm> results = new HashSet<>();

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
        Collection<GOTerm> descendants = descendantsCache.get( entry );

        if ( descendants == null ) {
            descendants = new HashSet<>();

            Collection<GOTerm> children = getChildren( entry, includePartOf );
            for ( GOTerm child : children ) {
                descendants.add( child );
                descendants.addAll( getDescendants( child, includePartOf ) );
            }

            descendants = Collections.unmodifiableCollection( descendants );

            descendantsCache.put( entry, descendants );

        }

        return new HashSet<>( descendants );
    }

    @Override
    public Collection<GOTerm> getAncestors( GOTerm entry ) {
        return getAncestors( entry, true );
    }

    @Override
    public Collection<GOTerm> getAncestors( GOTerm entry, boolean includePartOf ) {
        Collection<GOTerm> ancestors = new HashSet<>();

        Collection<GOTerm> parents = getParents( entry, includePartOf );
        for ( GOTerm parent : parents ) {
            ancestors.add( parent );
            ancestors.addAll( getAncestors( parent, includePartOf ) );
        }

        ancestors = Collections.unmodifiableCollection( ancestors );

        return new HashSet<>( ancestors );
    }

    @Override
    public Collection<Gene> getGenes( String id, Taxon taxon ) {
        return getGenes( termMap.get( id ), taxon );
    }

    @Override
    public Collection<Gene> getGenes( GOTerm t, Taxon taxon ) {
        if ( t == null ) return null;
        Collection<GOTerm> descendants = getDescendants( t );
        Collection<Gene> results = new HashSet<>();
        Collection<GeneAnnotation> ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( t.getId(), taxon );
        if ( ga != null ) {
            results = geneAnnotationService.annotationToGene( ga );
        }

        for ( GOTerm term : descendants ) {
            ga = geneAnnotationService.findByGeneOntologyIdAndTaxon( term.getId(), taxon );
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

        Collection<GOTerm> allGOTermSet = new HashSet<>();

        Collection<GeneAnnotation> annotations = geneAnnotationService.findByGene( gene );
        if ( annotations != null ) {
            for ( GeneAnnotation ga : annotations ) {
                if ( !termMap.containsKey( ga.getGoId() ) ) {
                    log.warn( "Term " + ga.getGoId() + " not found in term map cant add to results" );
                    continue;
                }

                GOTerm term = termMap.get( ga.getGoId() );

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
    public Integer getGeneSize( GOTerm t ) {
        if ( t == null ) return null;
        return t.getSize();
    }


    @Override
    public Integer getGeneSizeInTaxon( String id, Taxon taxon ) {
        return getGeneSizeInTaxon( termMap.get( id ), taxon );
    }

    @Override
    public Integer getGeneSizeInTaxon( GOTerm t, Taxon taxon ) {
        if ( t == null ) return null;
        return t.getSize( taxon );
    }

    @Override
    public Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Taxon taxon ) {
        Collection<Gene> results = new HashSet<>();

        for ( GeneOntologyTerm term : goTerms ) {
            results.addAll( getGenes( termMap.get( term.getGeneOntologyId() ), taxon ) );
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
        log.info( "INIT" );
        if ( running.get() ) {
            log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        initializeGeneOntology();
    }

    private synchronized void initializeGeneOntology() {
        if ( running.get() ) return;

        Thread loadThread = new Thread( () -> {
            running.set( true );
            termMap = new HashMap<>();
            log.info( "Loading Gene Ontology..." );

            try {
                loadTermsInNameSpace( GO_URL );
                log.info( "Gene Ontology loaded, total of " + termMap.size() + " items." );

                long cacheSize = precomputeSizes();
                log.info( "GOSize cache size: " + cacheSize );

                ready.set( true );
                running.set( false );

                log.info( "Done loading GO" );

            } catch (Throwable e) {
                if ( log != null ) log.error( e, e );
                ready.set( false );
                running.set( false );
            }
        } );

        loadThread.start();

    }

    protected void loadTermsInNameSpace( String url ) throws IOException {
        InputStream input = new URL( url ).openStream();
        GOParser gOParser = new GOParser( input );
        termMap = gOParser.getMap();
    }

    private long precomputeSizes() {
        List<AggregateCount> list = geneAnnotationService.calculateDirectSizes();
        Map<GOTerm, Map<Taxon, Integer>> directTermSizeByTaxon = new HashMap<>();
        Collection<String> nullIds = new HashSet<>();
        for ( AggregateCount ac : list ) {
            GOTerm term = termMap.get( ac.getGeneOntologyId() );

            if ( term == null ) {
                nullIds.add( ac.getGeneOntologyId() );
                continue;
            }

            directTermSizeByTaxon.computeIfAbsent( term, k -> new HashMap<>() ).put( ac.getTaxon(), (int) ac.getCount() );

            // term.setSize(count, taxonId);

        }

        if ( nullIds.size() > 0 ) {
            log.warn( "Could not find " + nullIds.size() + " terms in map " );
        }

        for ( GOTerm term : termMap.values() ) {
            Map<Taxon, Integer> sizesByTaxon = new HashMap<>();

            sizesByTaxon = addMaps( sizesByTaxon, directTermSizeByTaxon.get( term ) );

            Collection<GOTerm> descendants = getDescendants( term );

            for ( GOTerm descendant : descendants ) {
                sizesByTaxon = addMaps( sizesByTaxon, directTermSizeByTaxon.get( descendant ) );
            }

            term.setSizesByTaxon( sizesByTaxon );

        }

        return directTermSizeByTaxon.size();

    }

    private Map<Taxon, Integer> addMaps( Map<Taxon, Integer> one, Map<Taxon, Integer> two ) {
        // Changes first Map in place

        if ( two == null ) return one;

        for ( Map.Entry<Taxon, Integer> entry : two.entrySet() ) {
            Taxon key = entry.getKey();
            Integer val = entry.getValue();
            one.merge( key, val, ( a, b ) -> a + b );
        }

        return one;
    }

    private void precomputeDescendants() {
        getDescendants( termMap.get( "GO:0003674" ) );
        getDescendants( termMap.get( "GO:0005575" ) );
        getDescendants( termMap.get( "GO:0008150" ) );
    }

    @Override
    public Collection<GOTerm> deserializeGOTerms( String[] GOJSON ) {
        Collection<GOTerm> results = new HashSet<>();
        for ( String aGOJSON : GOJSON ) {
            JSONObject json = new JSONObject( aGOJSON );
            if ( !json.has( "geneOntologyId" ) ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }
            String geneOntologyId = json.getString( "geneOntologyId" );

            if ( geneOntologyId == null || geneOntologyId.isEmpty() ) {
                throw new IllegalArgumentException( "Every gene must have an assigned ID." );
            }

            // GeneOntologyTerm term = geneOntologyTermCache.fetchById( geneOntologyId );
            GOTerm term = termMap.get( geneOntologyId );
            if ( !(term == null) ) {
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
        Collection<JSONObject> jsonList = new ArrayList<>();
        for ( GeneOntologyTerm term : goTerms ) {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put( "geneOntologyId", term.getGeneOntologyId() );
            jsonObj.put( "aspect", term.getAspect() );
            jsonObj.put( "geneOntologyTerm", term.getName() );
            jsonObj.put( "definition", term.getDefinition() );
            jsonObj.put( "taxonId", term.getTaxon().getId() );

            jsonObj.put( "size", term.getSize() );
            jsonObj.put( "frequency", term.getFrequency() );
            jsonList.add( jsonObj );
        }

        return new JSONArray( jsonList );
    }
}
