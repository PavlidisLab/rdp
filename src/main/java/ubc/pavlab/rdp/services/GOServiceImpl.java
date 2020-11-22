package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.model.enums.TermMatchType;
import ubc.pavlab.rdp.repositories.GeneOntologyTermInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.Gene2GoParser;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.SearchResult;

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

    @Autowired
    private GeneOntologyTermInfoRepository goRepository;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private OBOParser oboParser;

    @Autowired
    private Gene2GoParser gene2GoParser;

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
        Map<String, GeneOntologyTermInfo> goTerms = new HashMap<>();
        for ( Map.Entry<String, OBOParser.Term> entry : parsedTerms.entrySet() ) {
            goTerms.put( entry.getKey(), convertTerm( entry.getValue() ) );
        }
        return goTerms;
    }

    /**
     * TODO: store the terms in the database to avoid this initialization
     */
    @Override
    @PostConstruct
    public void updateGoTerms() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        log.info( MessageFormat.format( "Loading GO Terms from: {0}.", cacheSettings.getTermFile() ) );

        Map<String, GeneOntologyTermInfo> terms;
        try {
            terms = convertTerms( oboParser.parseStream( cacheSettings.getTermFile().getInputStream() ) );
        } catch ( IOException e ) {
            return;
        }

        log.info( MessageFormat.format( "Gene Ontology loaded, total of {0} items.", count() ) );

        log.info( MessageFormat.format( "Loading annotations from: {0}.", cacheSettings.getAnnotationFile() ) );

        try {
            Collection<Gene2GoParser.Record> records = gene2GoParser.populateAnnotations( new GZIPInputStream( cacheSettings.getAnnotationFile().getInputStream() ) );

            Map<String, List<Gene2GoParser.Record>> recordsByGoTerm = records.stream().collect( groupingBy( Gene2GoParser.Record::getGoId, Collectors.toList() ) );

            for ( Map.Entry<String, List<Gene2GoParser.Record>> entry : recordsByGoTerm.entrySet() ) {
                GeneOntologyTermInfo term = terms.get( entry.getKey() );

                if ( term == null ) {
                    log.warn( MessageFormat.format( "{0} is missing from {1}, its direct genes will be ignored.", entry.getKey(), cacheSettings.getTermFile() ) );
                    continue;
                }

                for ( Gene2GoParser.Record record : entry.getValue() ) {
                    GeneInfo gene = new GeneInfo();
                    Taxon taxon = new Taxon();
                    taxon.setId( record.getTaxonId() );
                    gene.setGeneId( record.getGeneId() );
                    gene.setTaxon( taxon );
                    term.getDirectGenes().add( gene );
                }
            }

            saveAlias( terms );

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
    public Map<GeneOntologyTermInfo, Long> termFrequencyMap( Collection<? extends Gene> genes ) {
        return genes.stream()
                .flatMap( g -> getTermsForGene( g, true, true ).stream() )
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
        return t.getDirectGenes().stream().filter( g -> g.getTaxon().equals( taxon ) ).count();
    }

    @Override
    public Collection<GeneOntologyTermInfo> getChildren( GeneOntologyTermInfo entry ) {
        return getChildren( entry, true );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getChildren( GeneOntologyTermInfo entry, boolean includePartOf ) {
        if ( entry == null ) {
            return null;
        }
        return entry.getChildren().stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTermInfo save( GeneOntologyTermInfo term ) {
        return goRepository.save( term );
    }

    @Override
    public Iterable<GeneOntologyTermInfo> save( Iterable<GeneOntologyTermInfo> terms ) {
        return goRepository.save( terms );
    }

    @Override
    public GeneOntologyTermInfo saveAlias( String goId, GeneOntologyTermInfo term ) {
        return goRepository.saveAlias( goId, term );
    }

    @Override
    public Iterable<GeneOntologyTermInfo> saveAlias( Map<String, GeneOntologyTermInfo> terms ) {
        return goRepository.saveAlias( terms );
    }

    @Override
    public void deleteAll() {
        goRepository.deleteAll();
    }

    @Override
    public long count() {
        return goRepository.count();
    }

    @Override
    public Collection<GeneOntologyTermInfo> getDescendants( GeneOntologyTermInfo entry ) {
        return getDescendants( entry, true );
    }

    @Override
    @Cacheable
    public Collection<GeneOntologyTermInfo> getDescendants( GeneOntologyTermInfo entry, boolean includePartOf ) {
        if ( entry == null ) {
            return null;
        }

        Collection<GeneOntologyTermInfo> descendants = new HashSet<>();

        for ( GeneOntologyTermInfo child : getChildren( entry, includePartOf ) ) {
            descendants.add( child );
            descendants.addAll( getDescendants( child, includePartOf ) );
        }

        return Collections.unmodifiableCollection( descendants );
    }

    @Override
    @Deprecated
    public Collection<GeneInfo> getDirectGenes( GeneOntologyTermInfo term ) {
        return term.getDirectGenes();
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( String id, Taxon taxon ) {
        if ( id == null || taxon == null ) return null;

        GeneOntologyTerm term = goRepository.findOne( id );
        if ( term == null ) return Collections.emptySet();

        return getGenesInTaxon( goRepository.findOne( id ), taxon );
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( GeneOntologyTermInfo t, Taxon taxon ) {
        if ( t == null || taxon == null ) return null;
        Collection<GeneOntologyTermInfo> descendants = new HashSet<>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGenes().stream() )
                .filter( gene -> gene.getTaxon().equals( taxon ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneInfo> getGenes( GeneOntologyTermInfo t ) {
        if ( t == null ) return null;
        Collection<GeneOntologyTermInfo> descendants = new HashSet<GeneOntologyTermInfo>( getDescendants( t ) );

        descendants.add( t );

        return descendants.stream()
                .flatMap( term -> term.getDirectGenes().stream() )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<GeneInfo> getGenesInTaxon( Collection<GeneOntologyTermInfo> goTerms, Taxon taxon ) {
        if ( goTerms == null || taxon == null ) return null;
        return goTerms.stream().flatMap( t -> getGenesInTaxon( t, taxon ).stream() ).collect( Collectors.toSet() );
    }

    @Override
    public GeneOntologyTermInfo getTerm( String goId ) {
        return goRepository.findOne( goId );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene ) {
        return goRepository.findAllByGene( gene );
    }

    @Override
    public Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene, boolean includePartOf, boolean propagateUpwards ) {

        Collection<GeneOntologyTermInfo> allGOTermSet = new HashSet<>();

        for ( GeneOntologyTermInfo term : goRepository.findAllByGene( gene ) ) {
            allGOTermSet.add( term );

            if ( propagateUpwards ) {
                allGOTermSet.addAll( term.getAncestors( includePartOf ) );
            }
        }

        return Collections.unmodifiableCollection( allGOTermSet );
    }
}
