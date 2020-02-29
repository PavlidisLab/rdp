package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GOParser;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.SearchResult;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.counting;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("goService")
@CommonsLog
public class GOServiceImpl implements GOService {

    private static final String GO_URL = "http://purl.obolibrary.org/obo/go.obo";
    private static final String GENE2GO_URL = "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/gene2go.gz";

    private Map<String, GeneOntologyTerm> termMap = new HashMap<>();

    private static Map<GeneOntologyTerm, Collection<GeneOntologyTerm>> descendantsCache = new HashMap<>();

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    TaxonService taxonService;

    @Autowired
    GeneService geneService;

    @PostConstruct
    private void initialize() {

        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        if ( cacheSettings.isEnabled() ) {
            try {
                if ( cacheSettings.isLoadFromDisk() ) {
                    log.info( "Loading GO Terms from disk: " + cacheSettings.getTermFile() );
                    setTerms( GOParser.parse( new File( cacheSettings.getTermFile() ) ) );
                } else {
                    log.info( "Loading GO Terms from URL: " + GO_URL );
                    setTerms( GOParser.parse( new URL( GO_URL ) ) );
                }

                log.info( "Gene Ontology loaded, total of " + size() + " items." );

                if ( cacheSettings.isLoadFromDisk() ) {
                    log.info( "Loading annotations from disk: " + cacheSettings.getAnnotationFile() );
                    Gene2GoParser.populateAnnotations( new File( cacheSettings.getAnnotationFile() ), taxonService.findByActiveTrue(), geneService, this );
                } else {
                    log.info( "Loading annotations from URL: " + GENE2GO_URL );
                    Gene2GoParser.populateAnnotations( new URL( GENE2GO_URL ), taxonService.findByActiveTrue(), geneService, this );
                }

                log.info( "Finished loading annotations" );

                for ( GeneOntologyTerm goTerm : getAllTerms() ) {
                    goTerm.setSizesByTaxon( getGenes( goTerm ).stream().collect(
                            Collectors.groupingBy(
                                    Gene::getTaxon, counting()
                            ) ) );
                }

                log.info( "Finished precomputing gene annotation sizes" );

            } catch (Exception e) {
                log.error( "Issue loading terms and/or annotations", e );
            }
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
    public Map<GeneOntologyTerm, Long> termFrequencyMap( Collection<? extends Gene> genes ) {
        if ( genes == null ) {
            return null;
        }
        return genes.stream()
                .flatMap( g -> g.getAllTerms( true, true ).stream() )
                .collect( Collectors.groupingBy( Function.identity(), counting() ) );
    }

    private SearchResult<GeneOntologyTerm> queryTerm( String queryString, GeneOntologyTerm term ) {
        if ( term.getGoId().equalsIgnoreCase( queryString ) || term.getGoId().equalsIgnoreCase( "GO:" + queryString ) ) {
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
        Stream<SearchResult<UserTerm>> stream = termMap.values().stream().filter( t -> t.getSize( taxon ) <= applicationSettings.getGoTermSizeLimit() )
                .map( t -> queryTerm( queryString, t ) )
                .filter( Objects::nonNull )
                .map( sr -> new SearchResult<>( sr.getMatchType(), new UserTerm( sr.getMatch(), taxon, null ) ) )
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
        if ( entry == null) {
            return null;
        }
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
        if ( entry == null) {
            return null;
        }

        Collection<GeneOntologyTerm> descendants = descendantsCache.get( entry );

        if ( descendants == null || !includePartOf) {
            descendants = new HashSet<>();

            for ( GeneOntologyTerm child : getChildren( entry, includePartOf ) ) {
                descendants.add( child );
                descendants.addAll( getDescendants( child, includePartOf ) );
            }

            descendants = Collections.unmodifiableCollection( descendants );

            if ( includePartOf ) {
                descendantsCache.put( entry, descendants );
            }

        }

        return new HashSet<>( descendants );
    }

    @Override
    public Collection<Gene> getGenes( String id, Taxon taxon ) {
        if ( id == null || taxon == null ) return null;

        GeneOntologyTerm term = termMap.get( id );
        if (term == null) return Collections.emptySet();

        return getGenes( termMap.get( id ), taxon );
    }

    @Override
    public Collection<Gene> getGenes( GeneOntologyTerm t, Taxon taxon ) {
        if ( t == null || taxon == null ) return null;
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
    public Collection<Gene> getGenes( Collection<? extends GeneOntologyTerm> goTerms, Taxon taxon ) {
        if (goTerms == null || taxon == null) return null;
        return goTerms.stream().flatMap( t -> getGenes( t, taxon ).stream() ).collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTerm getTerm( String goId ) {
        return termMap.get( goId );
    }

}
