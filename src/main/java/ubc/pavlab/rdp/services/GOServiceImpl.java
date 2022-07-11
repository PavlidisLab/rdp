package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTermInfo;
import ubc.pavlab.rdp.model.Relationship;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.repositories.GeneOntologyTermInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("goService")
@CommonsLog
public class GOServiceImpl implements GOService, InitializingBean {

    static final String
            ANCESTORS_CACHE_NAME = "ubc.pavlab.rdp.services.GOService.ancestors",
            DESCENDANTS_CACHE_NAME = "ubc.pavlab.rdp.services.GOService.descendants";

    @Autowired
    private GeneOntologyTermInfoRepository goRepository;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private OBOParser oboParser;

    @Autowired
    private Gene2GoParser gene2GoParser;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ResourceLoader resourceLoader;

    private Cache ancestorsCache;
    private Cache descendantsCache;

    @Override
    public void afterPropertiesSet() {
        ancestorsCache = CacheUtils.getCache( cacheManager, ANCESTORS_CACHE_NAME );
        descendantsCache = CacheUtils.getCache( cacheManager, DESCENDANTS_CACHE_NAME );
    }

    private static Relationship convertRelationship( OBOParser.Relationship parsedRelationship ) {
        return new Relationship( convertTermIgnoringRelationship( parsedRelationship.getNode() ),
                RelationshipType.valueOf( parsedRelationship.getRelationshipType().toString() ) );
    }

    private static GeneOntologyTermInfo convertTermIgnoringRelationship( OBOParser.Term parsedTerm ) {
        GeneOntologyTermInfo geneOntologyTerm = new GeneOntologyTermInfo();
        geneOntologyTerm.setGoId( parsedTerm.getId() );
        geneOntologyTerm.setName( parsedTerm.getName() );
        geneOntologyTerm.setDefinition( parsedTerm.getDefinition() );
        geneOntologyTerm.setAspect( Aspect.valueOf( parsedTerm.getNamespace() ) );
        return geneOntologyTerm;
    }

    private static GeneOntologyTermInfo convertTerm( OBOParser.Term parsedTerm ) {
        GeneOntologyTermInfo geneOntologyTerm = convertTermIgnoringRelationship( parsedTerm );
        geneOntologyTerm.setParents( parsedTerm.getParents().stream().map( GOServiceImpl::convertRelationship ).collect( Collectors.toSet() ) );
        geneOntologyTerm.setChildren( parsedTerm.getChildren().stream().map( GOServiceImpl::convertRelationship ).collect( Collectors.toSet() ) );
        return geneOntologyTerm;
    }

    private static Map<String, GeneOntologyTermInfo> convertTerms( Map<String, OBOParser.Term> parsedTerms ) {
        // using a stream does not work because the GO identifier does not constitute an injective mapping due to
        // aliases
        Map<String, GeneOntologyTermInfo> goTerms = new HashMap<>( parsedTerms.size() );
        for ( Map.Entry<String, OBOParser.Term> entry : parsedTerms.entrySet() ) {
            goTerms.put( entry.getKey(), convertTerm( entry.getValue() ) );
        }
        return goTerms;
    }

    /**
     * TODO: store the terms in the database to avoid this initialization
     */
    @Override
    public void updateGoTerms() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        log.info( MessageFormat.format( "Loading GO terms from: {0}.", cacheSettings.getTermFile() ) );

        if ( cacheSettings.getTermFile() == null || cacheSettings.getTermFile().isEmpty() ) {
            log.warn( "No term file is defined, skipping update of GO terms." );
            return;
        }

        if ( cacheSettings.getAnnotationFile() == null ) {
            log.warn( "No gene2go annotations file is defined, skipping update of GO terms." );
            return;
        }

        Map<String, GeneOntologyTermInfo> terms;
        try {
            terms = convertTerms( oboParser.parseStream( resourceLoader.getResource( cacheSettings.getTermFile() ).getInputStream() ) );
        } catch ( IOException | ParseException e ) {
            log.error( "Failed to parse GO terms.", e );
            return;
        }

        log.info( MessageFormat.format( "Loading gene2go annotations from: {0}.", cacheSettings.getAnnotationFile() ) );

        Collection<Gene2GoParser.Record> records;
        try {
            records = gene2GoParser.parse( new GZIPInputStream( cacheSettings.getAnnotationFile().getInputStream() ) );
        } catch ( IOException e ) {
            log.error( "Failed to retrieve gene2go annotations.", e );
            return;
        } catch ( ParseException e ) {
            log.error( "Failed to parse gene2go annotations.", e );
            return;
        }

        Map<String, List<Gene2GoParser.Record>> recordsByGoTerm = records.stream().collect( groupingBy( Gene2GoParser.Record::getGoId, Collectors.toList() ) );
        Set<String> missingFromTermFile = new HashSet<>();
        for ( Map.Entry<String, List<Gene2GoParser.Record>> entry : recordsByGoTerm.entrySet() ) {
            GeneOntologyTermInfo term = terms.get( entry.getKey() );

            if ( term == null ) {
                missingFromTermFile.add( entry.getKey() );
                continue;
            }

            for ( Gene2GoParser.Record record : entry.getValue() ) {
                term.getDirectGeneIds().add( record.getGeneId() );
                term.getDirectGeneIdsByTaxonId().add( record.getTaxonId(), record.getGeneId() );
            }
        }

        saveAlias( terms );

        // this tends to produce a lot of warnings, so we just warn for the first 5 or so
        for ( String goTerm : missingFromTermFile.stream().limit( 5 ).collect( Collectors.toSet() ) ) {
            log.warn( MessageFormat.format( "{0} is missing from {1}, its direct genes will be ignored.", goTerm, cacheSettings.getTermFile() ) );
        }
        if ( missingFromTermFile.size() > 5 ) {
            log.warn( MessageFormat.format( "{0} more terms were missing from {1}, their direct genes will also be ignored.", missingFromTermFile.size() - 5, cacheSettings.getTermFile() ) );
        }

        log.info( MessageFormat.format( "Done updating GO terms, total of {0} items.", count() ) );
    }

    @Override
    public Map<GeneOntologyTermInfo, Long> termFrequencyMap( Collection<? extends Gene> genes ) {
        return genes.stream()
                .flatMap( g -> getTermsForGene( g, true ).stream() )
                .collect( groupingBy( identity(), counting() ) );
    }

    private SearchResult<GeneOntologyTermInfo> queryTerm( String queryString, GeneOntologyTermInfo term ) {
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
    public List<SearchResult<GeneOntologyTermInfo>> search( String queryString, Taxon taxon, int max ) {
        Stream<SearchResult<GeneOntologyTermInfo>> stream = goRepository.findAll().stream()
                .filter( t -> getSizeInTaxon( t, taxon ) <= applicationSettings.getGoTermSizeLimit() )
                .map( t -> queryTerm( queryString, t ) )
                .filter( Objects::nonNull )
                .sorted( Comparator.comparingInt( sr -> sr.getMatchType().getOrder() ) );

        if ( max > -1 ) {
            stream = stream.limit( max );
        }

        return stream.collect( Collectors.toList() );
    }

    @Override
    public long getSizeInTaxon( GeneOntologyTermInfo t, Taxon taxon ) {
        return getGenesInTaxon( t, taxon ).size();
    }

    @Override
    public Collection<GeneOntologyTermInfo> getChildren( GeneOntologyTermInfo entry ) {
        return entry.getChildren().stream()
                .map( Relationship::getTerm )
                .map( this::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTermInfo save( GeneOntologyTermInfo term ) {
        try {
            return goRepository.save( term );
        } finally {
            evict( term );
        }
    }

    @Override
    public Iterable<GeneOntologyTermInfo> save( Iterable<GeneOntologyTermInfo> terms ) {
        try {
            return goRepository.saveAll( terms );
        } finally {
            evict( terms );
        }
    }

    @Override
    public GeneOntologyTermInfo saveAlias( String goId, GeneOntologyTermInfo term ) {
        try {
            return goRepository.saveByAlias( goId, term );
        } finally {
            evict( term );
        }
    }

    @Override
    public Iterable<GeneOntologyTermInfo> saveAlias( Map<String, GeneOntologyTermInfo> terms ) {
        try {
            return goRepository.saveAllByAlias( terms );
        } finally {
            evict( terms.values() );
        }
    }

    @Override
    public void deleteAll() {
        try {
            goRepository.deleteAll();
        } finally {
            evictAll();
        }
    }

    @Override
    public long count() {
        return goRepository.count();
    }

    @Override
    public Collection<GeneOntologyTermInfo> getDescendants( GeneOntologyTermInfo entry ) {
        return getDescendantsInternal( entry );
    }

    private Set<GeneOntologyTermInfo> getDescendantsInternal( GeneOntologyTermInfo entry ) {
        //noinspection unchecked
        Set<GeneOntologyTermInfo> results = descendantsCache.get( entry, Set.class );
        if ( results != null ) {
            return results;
        }
        results = new HashSet<>();
        for ( GeneOntologyTermInfo child : getChildren( entry ) ) {
            results.add( child );
            results.addAll( getDescendantsInternal( child ) );
        }
        descendantsCache.put( entry, results );
        return results;
    }

    /**
     * Obtain genes directly associated to this GO term.
     *
     * @param term
     * @return
     * @deprecated use {@link GeneOntologyTermInfo#getDirectGeneIds()} to obtain direct genes
     */
    @Override
    @Deprecated
    public Collection<Integer> getDirectGenes( GeneOntologyTermInfo term ) {
        return term.getDirectGeneIds();
    }

    @Override
    public Collection<Integer> getGenesInTaxon( String id, Taxon taxon ) {
        return goRepository.findById( id )
                .map( term -> getGenesInTaxon( term, taxon ) )
                .orElse( Collections.emptySet() );
    }

    @Override
    public Collection<Integer> getGenesInTaxon( GeneOntologyTermInfo t, Taxon taxon ) {
        Collection<GeneOntologyTermInfo> descendants = new HashSet<>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGeneIdsByTaxonId().getOrDefault( taxon.getId(), Collections.emptyList() ).stream() )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<Integer> getGenes( GeneOntologyTermInfo t ) {
        Collection<GeneOntologyTermInfo> descendants = new HashSet<GeneOntologyTermInfo>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGeneIds().stream() )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<Integer> getGenesInTaxon( Collection<GeneOntologyTermInfo> goTerms, Taxon taxon ) {
        return goTerms.stream().flatMap( t -> getGenesInTaxon( t, taxon ).stream() ).collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTermInfo getTerm( String goId ) {
        return goRepository.findById( goId ).orElse( null );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene ) {
        return goRepository.findByDirectGeneIdsContaining( gene.getGeneId() );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene, boolean propagateUpwards ) {

        Collection<GeneOntologyTermInfo> allGOTermSet = new HashSet<>();

        for ( GeneOntologyTermInfo term : goRepository.findByDirectGeneIdsContaining( gene.getGeneId() ) ) {
            allGOTermSet.add( term );

            if ( propagateUpwards ) {
                allGOTermSet.addAll( getAncestors( term ) );
            }
        }

        return Collections.unmodifiableCollection( allGOTermSet );
    }

    private Collection<GeneOntologyTermInfo> getParents( GeneOntologyTermInfo term ) {
        return term.getParents().stream()
                .map( Relationship::getTerm )
                .map( this::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getAncestors( GeneOntologyTermInfo term ) {
        return getAncestorsInternal( term );
    }

    private Collection<GeneOntologyTermInfo> getAncestorsInternal( GeneOntologyTermInfo term ) {
        //noinspection unchecked
        Set<GeneOntologyTermInfo> results = ancestorsCache.get( term, Set.class );
        if ( results != null )
            return results;
        results = new HashSet<>();
        for ( GeneOntologyTermInfo parent : getParents( term ) ) {
            results.add( parent );
            results.addAll( getAncestorsInternal( parent ) );
        }
        ancestorsCache.put( term, results );
        return results;
    }

    private void evict( GeneOntologyTermInfo term ) {
        evict( Collections.singleton( term ) );
    }

    private void evict( Iterable<GeneOntologyTermInfo> terms ) {
        Set<GeneOntologyTermInfo> termsToEvict = new HashSet<>();
        for ( GeneOntologyTermInfo term : terms ) {
            termsToEvict.add( term );
        }

        log.info( String.format( "Evicting %d terms from the GO ancestors and descendants caches.", termsToEvict.size() ) );

        // first, let's retrieve what we already have in the cache
        Collection<GeneOntologyTermInfo> ancestors = termsToEvict.stream()
                .map( this::getAncestors )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
        Collection<GeneOntologyTermInfo> descendants = termsToEvict.stream()
                .map( this::getDescendants )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );

        // evict the terms from both caches
        for ( GeneOntologyTermInfo term : termsToEvict ) {
            ancestorsCache.evict( term );
            descendantsCache.evict( term );
        }

        // all the ancestors mention one of the updated terms in their descendants, so we evict those
        for ( GeneOntologyTermInfo ancestor : ancestors ) {
            descendantsCache.evict( ancestor );
        }

        // conversely, all the descendants mention one of the updated term in their ancestors
        for ( GeneOntologyTermInfo descendant : descendants ) {
            ancestorsCache.evict( descendant );
        }
    }

    private void evictAll() {
        log.info( "Evicting all the terms from ancestors and descendants caches." );
        ancestorsCache.clear();
        descendantsCache.clear();
    }
}
