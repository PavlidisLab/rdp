package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Relationship;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.SearchResult;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
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
public class GOServiceImpl implements GOService {

    private Map<String, GeneOntologyTerm> termMap = new HashMap<>();

    private MultiValueMap<Integer, GeneOntologyTerm> geneMap = new LinkedMultiValueMap<>();

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private GeneInfoService geneInfoService;

    @Autowired
    private OBOParser oboParser;

    @Autowired
    private Gene2GoParser gene2GoParser;

    private static Relationship convertRelationship( OBOParser.Relationship parsedRelationship ) {
        return new Relationship( convertTermIgnoringRelationship( parsedRelationship.getNode() ),
                RelationshipType.valueOf( parsedRelationship.getRelationshipType().toString() ) );
    }

    private static GeneOntologyTerm convertTermIgnoringRelationship( OBOParser.Term parsedTerm ) {
        GeneOntologyTerm geneOntologyTerm = new GeneOntologyTerm();
        geneOntologyTerm.setGoId( parsedTerm.getId() );
        geneOntologyTerm.setName( parsedTerm.getName() );
        geneOntologyTerm.setDefinition( parsedTerm.getDefinition() );
        geneOntologyTerm.setAspect( Aspect.valueOf( parsedTerm.getNamespace() ) );
        return geneOntologyTerm;
    }

    private static GeneOntologyTerm convertTerm( OBOParser.Term parsedTerm ) {
        GeneOntologyTerm geneOntologyTerm = convertTermIgnoringRelationship( parsedTerm );
        geneOntologyTerm.setParents( parsedTerm.getParents().stream().map( GOServiceImpl::convertRelationship ).collect( Collectors.toSet() ) );
        geneOntologyTerm.setChildren( parsedTerm.getChildren().stream().map( GOServiceImpl::convertRelationship ).collect( Collectors.toSet() ) );
        return geneOntologyTerm;
    }

    private static Map<String, GeneOntologyTerm> convertTerms( Map<String, OBOParser.Term> parsedTerms ) {
        // using a stream does not work because the GO identifier does not constitute an injective mapping due to
        // aliases
        Map<String, GeneOntologyTerm> goTerms = new HashMap<>();
        for ( Map.Entry<String, OBOParser.Term> entry : parsedTerms.entrySet() ) {
            goTerms.put( entry.getKey(), convertTerm( entry.getValue() ) );
        }
        return goTerms;
    }

    /**
     * TODO: store the terms in the database to avoid this initialization
     */
    @Override
    @Transactional
    @PostConstruct
    public void updateGoTerms() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        log.info( MessageFormat.format( "Loading GO Terms from: {0}.", cacheSettings.getTermFile() ) );

        try {
            setTerms( convertTerms( oboParser.parseStream( cacheSettings.getTermFile().getInputStream() ) ) );
        } catch ( IOException e ) {
            return;
        }

        log.info( MessageFormat.format( "Gene Ontology loaded, total of {0} items.", getTerms().size() ) );

        log.info( MessageFormat.format( "Loading annotations from: {0}.", cacheSettings.getAnnotationFile() ) );

        try {
            Collection<Gene2GoParser.Record> records = gene2GoParser.populateAnnotations( new GZIPInputStream( cacheSettings.getAnnotationFile().getInputStream() ) );

            Map<String, List<Gene2GoParser.Record>> recordsByGoTerm = records.stream().collect( groupingBy( Gene2GoParser.Record::getGoId, Collectors.toList() ) );

            for ( Map.Entry<String, List<Gene2GoParser.Record>> entry : recordsByGoTerm.entrySet() ) {
                GeneOntologyTerm term = getTerm( entry.getKey() );

                if ( term == null ) {
                    continue;
                }

                for ( Gene2GoParser.Record record : entry.getValue() ) {
                    term.getDirectGeneIds().add( record.getGeneId() );
                }

                term.setSizesByTaxonId( entry.getValue().stream()
                        .collect( groupingBy( Gene2GoParser.Record::getTaxonId, counting() ) ) );
            }

            log.info( "Finished loading annotations." );
        } catch ( IOException e ) {
            log.error( "Failed to retrieve gene2go annotations.", e );
            return;
        } catch ( ParseException e ) {
            log.error( "Failed to parse gene2go annotations.", e );
            return;
        }

        log.info( "Done loading GO terms." );
    }

    @Override
    public void setTerms( Map<String, GeneOntologyTerm> termMap ) {
        this.termMap = termMap;
        geneMap = new LinkedMultiValueMap<>();
        for ( GeneOntologyTerm goTerm : termMap.values() ) {
            for ( Integer geneId : goTerm.getDirectGeneIds() ) {
                geneMap.add( geneId, goTerm );
            }
        }
    }

    @Override
    public Map<String, GeneOntologyTerm> getTerms() {
        return Collections.unmodifiableMap( termMap );
    }

    @Override
    public Map<GeneOntologyTerm, Long> termFrequencyMap( Collection<GeneInfo> genes ) {
        return genes.stream()
                .flatMap( g -> getAllTermsForGene( g, true, true ).stream() )
                .collect( groupingBy( identity(), counting() ) );
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
    public List<SearchResult<GeneOntologyTerm>> search( String queryString, Taxon taxon, int max ) {
        Stream<SearchResult<GeneOntologyTerm>> stream = termMap.values().stream()
                .filter( t -> t.getSizeInTaxon( taxon ) <= applicationSettings.getGoTermSizeLimit() )
                .map( t -> queryTerm( queryString, t ) )
                .filter( Objects::nonNull )
                .map( sr -> new SearchResult<>( sr.getMatchType(), sr.getMatch() ) )
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
        if ( entry == null ) {
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
    @Cacheable
    public Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf ) {
        if ( entry == null ) {
            return null;
        }

        Collection<GeneOntologyTerm> descendants = new HashSet<>();

        for ( GeneOntologyTerm child : getChildren( entry, includePartOf ) ) {
            descendants.add( child );
            descendants.addAll( getDescendants( child, includePartOf ) );
        }

        return Collections.unmodifiableCollection( descendants );
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( String id, Taxon taxon ) {
        if ( id == null || taxon == null ) return null;

        GeneOntologyTerm term = termMap.get( id );
        if ( term == null ) return Collections.emptySet();

        return getGenesInTaxon( termMap.get( id ), taxon );
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( GeneOntologyTerm t, Taxon taxon ) {
        if ( t == null || taxon == null ) return null;
        Collection<GeneOntologyTerm> descendants = new HashSet<>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGeneIds().stream() )
                .map( geneInfoService::load )
                .filter( Objects::nonNull )
                .filter( g -> g.getTaxon().equals( taxon ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneInfo> getGenes( GeneOntologyTerm t ) {
        if ( t == null ) return null;
        Collection<GeneOntologyTerm> descendants = new HashSet<>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGeneIds().stream() )
                .map( geneInfoService::load )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( Collection<GeneOntologyTerm> goTerms, Taxon taxon ) {
        if ( goTerms == null || taxon == null ) return null;
        return goTerms.stream().flatMap( t -> getGenesInTaxon( t, taxon ).stream() ).collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTerm getTerm( String goId ) {
        return termMap.get( goId );
    }

    @Override
    public Collection<GeneOntologyTerm> getTermsForGene( GeneInfo gene ) {
        return geneMap.getOrDefault( gene.getGeneId(), Lists.emptyList() );
    }

    @Override
    public Collection<GeneOntologyTerm> getAllTermsForGene( GeneInfo gene, boolean includePartOf, boolean propagateUpwards ) {

        Collection<GeneOntologyTerm> allGOTermSet = new HashSet<>();

        for ( GeneOntologyTerm term : geneMap.getOrDefault( gene.getGeneId(), Lists.emptyList() ) ) {
            allGOTermSet.add( term );

            if ( propagateUpwards ) {
                allGOTermSet.addAll( term.getAncestors( includePartOf ) );
            }
        }

        return Collections.unmodifiableCollection( allGOTermSet );
    }
}
