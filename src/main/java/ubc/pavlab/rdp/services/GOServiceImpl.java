package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GOParser;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.SearchResult;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("goService")
public class GOServiceImpl implements GOService {

    private static Log log = LogFactory.getLog( GOServiceImpl.class );

    private static final String GO_URL = "http://purl.obolibrary.org/obo/go.obo";
    private static final String GENE2GO_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz";

    private Map<String, GeneOntologyTerm> termMap = new HashMap<>();

    private static Map<GeneOntologyTerm, Collection<GeneOntologyTerm>> descendantsCache = new HashMap<>();

    private static int GO_SIZE_LIMIT = 100;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    TaxonService taxonService;

    @Autowired
    GeneService geneService;

    @PostConstruct
    private void initialize() {

        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();
        try {
            if (cacheSettings.isLoadFromDisk()) {
                log.info( "Loading GO Terms from disk: " + cacheSettings.getTermFile() );
                setTerms( GOParser.parse( new File( cacheSettings.getTermFile() ) ) );
            } else {
                log.info( "Loading GO Terms from URL: " + GO_URL );
                setTerms( GOParser.parse( new URL( GO_URL ) ) );
            }

            log.info( "Gene Ontology loaded, total of " + size() + " items." );

            if (cacheSettings.isLoadFromDisk()) {
                log.info( "Loading annotations from disk: " + cacheSettings.getAnnotationFile() );
                Gene2GoParser.populateAnnotations( new File( cacheSettings.getAnnotationFile() ), taxonService.findByActiveTrue(), geneService, this );
            } else {
                log.info( "Loading annotations from URL: " + GENE2GO_URL );
                Gene2GoParser.populateAnnotations( new URL(GENE2GO_URL), taxonService.findByActiveTrue(), geneService, this );
            }

            log.info( "Finished loading annotations" );

            for ( GeneOntologyTerm goTerm : getAllTerms() ) {
                goTerm.setSizesByTaxon( getGenes( goTerm ).stream().collect(
                        Collectors.groupingBy(
                                Gene::getTaxon, Collectors.counting()
                        ) ) );
            }

            log.info( "Finished precomputing gene annotation sizes" );

        } catch (Exception e) {
            log.error( "Issue loading terms and/or annotations", e );
        }


    }

    @Override
    public void setTerms( Map<String, GeneOntologyTerm> termMap ) {
        this.termMap = termMap;
    }

    @Override
    public Collection<GeneOntologyTerm> getAllTerms() {
        return termMap.values();
    }

    @Override
    public int size() {
        return termMap.size();
    }

    @Override
    public Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon, Set<Gene> genes ) {
        List<UserTerm> newTerms = new ArrayList<>();
        for ( GeneOntologyTerm goTerm : goTerms ) {
            UserTerm term = convertTermTypes( goTerm, taxon, genes );
            if ( term != null ) {
                newTerms.add( term );
            }
        }
        return newTerms;
    }

    @Override
    public UserTerm convertTermTypes( GeneOntologyTerm goTerm, Taxon taxon, Set<Gene> genes ) {
        if ( goTerm != null ) {
            UserTerm term = new UserTerm( goTerm, taxon, genes );
            if ( term.getSize() < GO_SIZE_LIMIT ) {
                return term;
            }
        }

        return null;
    }

    @Override
    public List<GeneOntologyTerm> recommendTerms( Collection<Gene> genes ) {
        return new ArrayList<>( calculateGoTermFrequency( genes, 2, 10, GO_SIZE_LIMIT ).keySet() );
    }

    private LinkedHashMap<GeneOntologyTerm, Integer> calculateGoTermFrequency( Collection<Gene> genes, int minimumFrequency,
                                                                               int minimumTermSize, int maximumTermSize ) {
        Map<GeneOntologyTerm, Integer> frequencyMap = new HashMap<>();
        for ( Gene g : genes ) {
            for ( GeneOntologyTerm term : g.getAllTerms( true, true ) ) {
                // Count
                frequencyMap.merge( term, 1, ( oldValue, one ) -> oldValue + one );
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
            // TODO: Restrict by size in taxon only?
            frequencyMap.entrySet().removeIf( termEntry -> termEntry.getKey().getTotalSize() < minimumTermSize || termEntry.getKey().getTotalSize() > maximumTermSize );
        }
        log.debug( "Limit by size done." );

        // Reduce GO TERM redundancies
        frequencyMap = reduceRedundancies( frequencyMap );

        log.debug( "Reduce redundancy done." );

        return frequencyMap.entrySet().stream()
                .sorted( Map.Entry.<GeneOntologyTerm, Integer>comparingByValue().reversed() ).
                        collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue,
                                ( e1, e2 ) -> e1, LinkedHashMap::new ) );
    }

    private Map<GeneOntologyTerm, Integer> reduceRedundancies( Map<GeneOntologyTerm, Integer> frequencyMap ) {
        // Reduce GO TERM redundancies
        Collection<GeneOntologyTerm> markedForDeletion = new HashSet<>();
        Map<GeneOntologyTerm, Integer> fmap = new HashMap<>( frequencyMap );
        for ( Map.Entry<GeneOntologyTerm, Integer> entry : fmap.entrySet() ) {
            GeneOntologyTerm term = entry.getKey();
            Integer frequency = entry.getValue();

            if ( markedForDeletion.contains( term ) ) {
                // Skip because a previous step would have taken care of this terms parents anyways
                continue;
            }

            for ( GeneOntologyTerm parent : term.getAncestors( true ) ) {
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
        for ( GeneOntologyTerm redundantTerm : markedForDeletion ) {
            fmap.remove( redundantTerm );
        }

        return fmap;
    }

    private SearchResult<GeneOntologyTerm> queryTerm( String queryString, GeneOntologyTerm term ) {
        if ( term.getId().equalsIgnoreCase( queryString ) || term.getId().equalsIgnoreCase( "GO:" + queryString ) ) {
            return new SearchResult<>( TermMatchType.EXACT_ID, term );
        }

        String pattern = "(?i:.*" + Pattern.quote( queryString ) + ".*)";
        if ( term.getName().matches( pattern ) ) {
            return new SearchResult<>( TermMatchType.NAME_CONTAINS, term );
        }

        if ( term.getDefinition().matches( pattern ) ) {
            return new SearchResult<>( TermMatchType.DEFINITION_CONTAINS, term );
        }

        List<String> splitPatternlist = Arrays.stream( queryString.split( " " ) )
                .filter( s -> !s.equals( "" ) )
                .map( s -> "(?i:.*" + Pattern.quote( s ) + ".*)" ).collect( Collectors.toList() );

        for ( String splitPattern : splitPatternlist ) {
            if ( term.getName().matches( splitPattern ) ) {
                return new SearchResult<>( TermMatchType.NAME_CONTAINS_PART, term );
            }
        }

        for ( String splitPattern : splitPatternlist ) {
            if ( term.getDefinition().matches( splitPattern ) ) {
                return new SearchResult<>( TermMatchType.DEFINITION_CONTAINS_PART, term );
            }
        }
        return null;
    }

    @Override
    public List<SearchResult<UserTerm>> search( String queryString, Taxon taxon, int max ) {
        Stream<SearchResult<UserTerm>> stream = termMap.values().stream().filter( t -> t.getSize( taxon ) <= GO_SIZE_LIMIT )
                .map( t -> queryTerm( queryString, t ) )
                .filter( Objects::nonNull )
                .map(sr -> new SearchResult<>( sr.getMatchType(), new UserTerm( sr.getMatch(), taxon, null )))
                .sorted( Comparator.comparingInt( sr -> sr.getMatchType().getOrder() ) );

        if ( max > -1 ) {
            stream = stream.limit( max );
        }

        return stream.collect( Collectors.toList() );
    }

    @Override
    public Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry ) {
        return getChildren( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf ) {
        return entry.getChildren().stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry ) {
        return getDescendants( entry, true );
    }

    @Override
    public Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf ) {

        Collection<GeneOntologyTerm> descendants = descendantsCache.get( entry );

        if ( descendants == null ) {
            descendants = new HashSet<>();

            for ( GeneOntologyTerm child : getChildren( entry, includePartOf ) ) {
                descendants.add( child );
                descendants.addAll( getDescendants( child, includePartOf ) );
            }

            descendants = Collections.unmodifiableCollection( descendants );

            descendantsCache.put( entry, descendants );

        }

        return new HashSet<>( descendants );
    }

    @Override
    public Collection<Gene> getGenes( String id, Taxon taxon ) {
        return getGenes( termMap.get( id ), taxon );
    }

    @Override
    public Collection<Gene> getGenes( GeneOntologyTerm t, Taxon taxon ) {
        if ( t == null ) return null;
        Collection<GeneOntologyTerm> descendants = getDescendants( t );

        descendants.add( t );

        return descendants.stream().flatMap( term -> term.getDirectGenes().stream() ).filter( g -> g.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @Override
    public Collection<Gene> getGenes( GeneOntologyTerm t ) {
        if ( t == null ) return null;
        Collection<GeneOntologyTerm> descendants = getDescendants( t );

        descendants.add( t );

        return descendants.stream().flatMap( term -> term.getDirectGenes().stream() ).collect( Collectors.toSet() );
    }

    @Override
    public Collection<UserGene> getRelatedGenes( Collection<? extends GeneOntologyTerm> goTerms, Taxon taxon ) {
        Collection<UserGene> results = new HashSet<>();

        for ( GeneOntologyTerm term : goTerms ) {
            results.addAll( getGenes( term, taxon ).stream().map( g -> new UserGene( g, TierType.TIER3 ) ).collect( Collectors.toSet() ) );
        }

        return results;
    }

    @Override
    public GeneOntologyTerm getTerm( String goId ) {
        return termMap.get( goId );
    }

}
