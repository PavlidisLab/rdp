package ubc.pavlab.rdp.services;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.exception.SQLGrammarException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.ontology.*;
import ubc.pavlab.rdp.repositories.ontology.OntologyRepository;
import ubc.pavlab.rdp.repositories.ontology.OntologyTermInfoRepository;
import ubc.pavlab.rdp.util.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * /**
 * This service combines both {@link OntologyTermInfoRepository} and {@link OntologyRepository}.
 *
 * @author poirgui
 */
@Service
@CommonsLog
public class OntologyService implements InitializingBean {

    private final OntologyRepository ontologyRepository;
    private final OntologyTermInfoRepository ontologyTermInfoRepository;

    private final MessageSource messageSource;

    private boolean isFullTextSupported = false;

    @Autowired
    public OntologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository, MessageSource messageSource ) {
        this.ontologyRepository = ontologyRepository;
        this.ontologyTermInfoRepository = ontologyTermInfoRepository;
        this.messageSource = messageSource;
    }

    /**
     * I'd guess this is the most robust way to determine of the match() ... against() syntax works.
     */
    @Override
    public void afterPropertiesSet() {
        try {
            ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndNameMatch( Collections.singleton( 1 ), "test" );
            this.isFullTextSupported = true;
            log.info( "Full-text is supported by the database engine, it will be used for querying ontology terms efficiently." );
        } catch ( InvalidDataAccessResourceUsageException e ) {
            if ( e.getCause() instanceof SQLGrammarException ) {
                this.isFullTextSupported = false;
                log.warn( "Full-text is not supported." );
            } else {
                throw e;
            }
        }
    }

    public Ontology findById( Integer id ) {
        return ontologyRepository.findOne( id );
    }

    public Ontology findByName( String name ) {
        return ontologyRepository.findByName( name );
    }

    @Transactional(readOnly = true)
    public List<OntologyTermInfo> findAllTermsByIdIn( List<Integer> ontologyTermIds ) {
        return ontologyTermIds.stream().map( ontologyTermInfoRepository::findOne ).collect( Collectors.toList() );
    }

    public List<Ontology> findAllOntologies() {
        return ontologyRepository.findAllByActiveTrue().stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    public List<Ontology> findAllOntologiesIncludingInactive() {
        return ontologyRepository.findAll().stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    public Stream<OntologyTermInfo> findAllTerms() {
        return ontologyTermInfoRepository.findAllByActiveTrueAndOntologyActiveTrue();
    }

    public Page<OntologyTermInfo> findAllTermsByOntology( Ontology ontology, Pageable pageable ) {
        return ontologyTermInfoRepository.findAllByActiveTrueAndOntology( ontology, pageable );
    }

    /**
     * Indicate if a collection of user terms contains a given ontology term.
     * <p>
     * This is necessary because {@link UserOntologyTerm} and {@link OntologyTermInfo} do not have the same definition
     * of equality.
     */
    public boolean userTermsContains( Collection<UserOntologyTerm> userOntologyTerms, OntologyTerm term ) {
        return userOntologyTerms.stream().anyMatch( ut -> ut.getOntology().equals( term.getOntology() ) && ut.getTermId().equals( term.getTermId() ) );
    }

    /**
     * Count the number of active ontologies.
     */
    public long count() {
        return ontologyRepository.countByActiveTrue();
    }

    /**
     * Count the number of active terms.
     */
    public long countTerms() {
        return ontologyTermInfoRepository.countByActiveTrue();
    }

    @Secured("ROLE_ADMIN")
    @Transactional(readOnly = true)
    public boolean existsByName( String name ) {
        return ontologyRepository.existsByName( name );
    }

    /**
     * Create an ontology from an OBO formatted input.
     *
     * @param reader an OBO formatted input
     * @return the created ontology
     * @throws org.springframework.dao.DataIntegrityViolationException if an ontology with the same name already exists
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public Ontology createFromObo( Reader reader ) throws IOException, ParseException, OntologyNameAlreadyUsedException {
        OBOParser.ParsingResult parsingResult = new OBOParser().parse( reader );
        String ontologyName = parsingResult.getOntology().getName();
        if ( ontologyRepository.existsByName( ontologyName ) ) {
            throw new OntologyNameAlreadyUsedException( ontologyName );
        }
        Ontology ontology = Ontology.builder( ontologyName ).build();
        return saveFromObo( ontology, parsingResult );
    }

    /**
     * Update an ontology from an OBO formatted input.
     *
     * @param ontology ontology to be updated
     * @param reader   OBO formatted input
     * @return the updated ontology
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public Ontology updateFromObo( Ontology ontology, Reader reader ) throws IOException, ParseException {
        OBOParser.ParsingResult parsingResult = new OBOParser().parse( reader );
        return saveFromObo( ontology, parsingResult );
    }

    private Ontology saveFromObo( Ontology ontology, OBOParser.ParsingResult parsingResult ) {
        // TODO: maybe let Hibernate do the mapping via a SortedMap?
        Map<String, OntologyTermInfo> existingTermsById = ontology.getTerms().stream()
                .collect( Collectors.toMap( OntologyTermInfo::getTermId, t -> t ) );

        Map<String, OntologyTermInfo> convertedTerms = new HashMap<>();

        // first pass for saving terms
        int i = 0;
        for ( OBOParser.Term term : parsingResult.getTerms() ) {
            assert term.getId() != null;
            assert term.getName() != null;
            OntologyTermInfo t = existingTermsById.get( term.getId() );
            if ( t == null ) {
                t = OntologyTermInfo.builder( ontology, term.getId() )
                        .name( term.getName() )
                        .ordering( ++i )
                        .build();
            }
            t.setDefinition( term.getDefinition() );

            t.getAltTermIds().clear();
            t.getAltTermIds().addAll( term.getAltIds() );

            t.getSynonyms().clear();
            for ( OBOParser.Term.Synonym s : term.getSynonyms() ) {
                if ( s.getSynonym().length() > OntologyTermInfo.MAX_SYNONYM_LENGTH ) {
                    log.warn( String.format( "Synonym %s for term %s is too wide (%d characters) to fit in the database, it will be ignored.",
                            s, term, s.getSynonym().length() ) );
                    continue;
                }
                t.getSynonyms().add( s.getSynonym() );
            }

            t.setObsolete( term.getObsolete() != null && term.getObsolete() );
            convertedTerms.put( t.getTermId(), t );
        }

        // second pass for setting relationships
        for ( OBOParser.Term term : parsingResult.getTerms() ) {
            SortedSet<OntologyTermInfo> subTerms = term.getInverseRelationships().stream()
                    .filter( rel -> rel.getTypedef().equals( OBOParser.Typedef.IS_A ) )
                    .map( OBOParser.Term.Relationship::getNode )
                    .map( OBOParser.Term::getId )
                    .map( convertedTerms::get )
                    .collect( Collectors.toCollection( TreeSet::new ) );
            convertedTerms.get( term.getId() ).setSubTerms( subTerms );
        }

        ontology.getTerms().addAll( convertedTerms.values() );

        return ontologyRepository.save( ontology );
    }

    /**
     * Efficiently activate an ontology and all of its terms.
     * <p>
     * Obsolete terms are ignored.
     *
     * @return the number of activated terms in the ontology
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public int activate( Ontology ontology ) {
        ontologyRepository.activate( ontology );
        return ontologyTermInfoRepository.activateByOntologyAndObsoleteFalse( ontology );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void deactivate( Ontology ontology ) {
        ontology.setActive( false );
        ontologyRepository.save( ontology );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void activateTerm( OntologyTermInfo ontologyTermInfo ) {
        ontologyTermInfo.setActive( true );
        ontologyTermInfoRepository.save( ontologyTermInfo );
    }

    /**
     * Activate the ontology subtree rooted in the given term.
     * <p>
     * The algorithm that does the activation handles potential cycles when traversing the subtree.
     * <p>
     * Note that obsolete terms are left out. If you need to activate obsolete terms, consider using
     * {@link #activateTerm(OntologyTermInfo)} explicitly instead.
     *
     * @param ontologyTermInfo the root of the subtree to activate where children are visite by {@link OntologyTermInfo#getSubTerms()}.
     * @return the number of activated terms
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public int activateTermSubtree( OntologyTermInfo ontologyTermInfo ) {
        Set<Integer> fringe = new HashSet<>();
        Set<Integer> V = new HashSet<>();
        fringe.add( ontologyTermInfo.getId() );
        while ( !fringe.isEmpty() ) {
            List<Integer> subTermIds = ontologyTermInfoRepository.findSubTermsIdsByTermIdIn( fringe );
            V.addAll( fringe );
            // remove already visited nodes
            subTermIds.removeIf( V::contains );
            // set the fringe
            fringe.clear();
            fringe.addAll( subTermIds );
        }
        return ontologyTermInfoRepository.activateByTermIdsAndObsoleteFalse( V );
    }

    @Transactional
    public void updateOntologies() {
        log.info( "Updating all active ontologies..." );
        for ( Ontology ontology : findAllOntologies() ) {
            if ( ontology.getOntologyUrl() != null ) {
                Resource resource = new UrlResource( ontology.getOntologyUrl() );
                log.info( "Updating " + ontology + " from " + resource + "..." );
                try ( Reader reader = new InputStreamReader( resource.getInputStream() ) ) {
                    updateFromObo( ontology, reader );
                    log.info( "Updated " + ontology + " from " + resource + "." );
                } catch ( IOException | ParseException e ) {
                    log.error( "Failed to update " + ontology + ".", e );
                }
            }
        }
        log.info( "Ontologies have been successfully updated." );
    }

    @Transactional(readOnly = true)
    public void writeObo( Ontology ontology, Writer writer ) throws IOException {
        try ( LineWriter bw = new LineWriter( writer ) ) {
            bw.writeLine( "format-version: 1.2" );
            bw.writeLine( "ontology: " + ontology.getName() );
            for ( OntologyTermInfo term : ontology.getTerms() ) {
                bw.newLine();
                bw.writeLine( "[Term]" );
                bw.writeLine( "id: " + term.getTermId() );
                bw.writeLine( "name: " + term.getName() );
                if ( term.getDefinition() != null ) {
                    bw.writeLine( "def: " + '"' + term.getDefinition() + '"' + " []" );
                }
                for ( OntologyTermInfo superTerm : term.getSuperTerms() ) {
                    bw.writeLine( "is_a: " + superTerm.getTermId() + " ! " + superTerm.getName() );
                }
                if ( term.getOrdering() != null ) {
                    bw.writeLine( "relationship: has_order {order=" + '"' + term.getOrdering() + '"' + "}" );
                }
            }

            bw.newLine();
            bw.writeLine( "[Typedef]" );
            bw.writeLine( "id: has_order" );
            bw.writeLine( "name: has_order" );
            bw.writeLine( "def: " + '"' + "Term has relative order to other terms defined at the same subclass level." + '"' + " []" );
            bw.writeLine( "is_reflexive: true" );
            bw.writeLine( "is_metadata_tag: true" );
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void delete( List<Ontology> ontologies ) {
        ontologyRepository.delete( ontologies );
    }

    @Transactional(readOnly = true)
    public Collection<SearchResult<OntologyTermInfo>> autocomplete( String query, Ontology ontology, int maxResults, Locale locale ) {
        return autocomplete( query, Collections.singleton( ontology ), maxResults, locale );
    }

    @Transactional(readOnly = true)
    public Collection<SearchResult<OntologyTermInfo>> autocomplete( String query, int maxResults, Locale locale ) {
        return autocomplete( query, new HashSet<>( ontologyRepository.findAllByActiveTrue() ), maxResults, locale );
    }

    private Collection<SearchResult<OntologyTermInfo>> autocomplete( String query, Set<Ontology> ontologies, int maxResults, Locale locale ) {
        List<SearchResult<OntologyTermInfo>> results = new ArrayList<>( maxResults );

        String normalizedQuery = TextUtils.normalize( query );

        Set<Integer> ontologyIds = ontologies.stream().map( Ontology::getId ).collect( Collectors.toSet() );

        if ( ontologies.isEmpty() || normalizedQuery.length() < 3 ) {
            return results;
        }

        // we want all the terms to appear and the last term treated as a prefix
        String fullTextQuery = TextUtils.tokenize( query ).stream()
                .filter( StringUtils::isAlpha ) // remove non-alpha tokens
                .map( t2 -> "+" + t2 ) // force the presence if sufficiently large
                .collect( Collectors.joining( " " ) );

        // make last term, if available a prefix match
        if ( fullTextQuery.length() > 0 ) {
            fullTextQuery += "*";
        }

        StopWatch timer = StopWatch.createStarted();
        StopWatch initialQueryTimer = StopWatch.createStarted();

        // ID
        results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndTermIdIgnoreCase( ontologies, normalizedQuery ).stream()
                .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_ID_EXACT, null, 1.0, locale ) )
                .collect( CollectionUtils.into( results ) );

        // alt IDs
        results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndAltTermIdsContainingIgnoreCase( ontologies, normalizedQuery ).stream()
                .map( t1 -> toSearchResult( t1, OntologyTermMatchType.ALT_ID_EXACT, String.join( ", ", t1.getAltTermIds() ), 1.0, locale ) )
                .collect( CollectionUtils.into( results ) );

        // term name
        if ( results.size() < maxResults ) {

            // then some occurrences in the term name
            // by full text (if supported)
            if ( isFullTextSupported ) {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndNameMatch( ontologyIds, fullTextQuery ).stream()
                        .map( row -> Pair.of( ontologyTermInfoRepository.findOne( (Integer) row[0] ), (Double) row[1] ) )
                        .map( t1 -> toSearchResult( t1.getFirst(), OntologyTermMatchType.TERM_NAME_MATCH, null, t1.getSecond() / TextUtils.tokenize( t1.getFirst().getName() ).size(), locale ) )
                        .collect( CollectionUtils.into( results ) );
            } else {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndNameIgnoreCase( ontologies, normalizedQuery ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_EXACT, null, 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndNameLikeIgnoreCase( ontologies, normalizedQuery + "%" ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_STARTS_WITH, null, 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndNameLikeIgnoreCase( ontologies, "%" + normalizedQuery + "%" ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_CONTAINS, null, 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
            }
        }

        // then by synonyms
        if ( results.size() < maxResults ) {
            // full text (if available)
            if ( isFullTextSupported ) {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndSynonymsMatch( ontologies, fullTextQuery ).stream()
                        .map( row -> Pair.of( ontologyTermInfoRepository.findOne( (Integer) row[0] ), (Double) row[1] ) )
                        .map( t1 -> toSearchResult( t1.getFirst(), OntologyTermMatchType.SYNONYM_MATCH, String.join( ", ", t1.getFirst().getSynonyms() ), t1.getSecond(), locale ) )
                        .collect( CollectionUtils.into( results ) );
            } else {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndSynonymsContainingIgnoreCase( ontologies, normalizedQuery ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_EXACT, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndSynonymsLikeIgnoreCase( ontologies, normalizedQuery + "%" ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_STARTS_WITH, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndSynonymsLikeIgnoreCase( ontologies, "%" + normalizedQuery + "%" ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_CONTAINS, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
            }
        }

        // then some definitions
        if ( results.size() < maxResults ) {
            if ( isFullTextSupported ) {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndDefinitionMatch( ontologyIds, fullTextQuery ).stream()
                        .map( row -> Pair.of( ontologyTermInfoRepository.findOne( (Integer) row[0] ), (Double) row[1] ) )
                        .map( t -> toSearchResult( t.getFirst(), OntologyTermMatchType.DEFINITION_MATCH, t.getFirst().getDefinition(), t.getSecond(), locale ) )
                        .collect( CollectionUtils.into( results ) );

            } else {
                results = ontologyTermInfoRepository.findAllByActiveTrueAndOntologyInAndDefinitionLikeIgnoreCase( ontologies, "%" + normalizedQuery + "%" ).stream()
                        .map( t -> toSearchResult( t, OntologyTermMatchType.DEFINITION_CONTAINS, t.getDefinition(), 0.0, locale ) )
                        .collect( CollectionUtils.into( results ) );
            }
        }

        initialQueryTimer.stop();

        StopWatch topoSortTimer = StopWatch.createStarted();
        Collection<SearchResult<OntologyTermInfo>> sortedResults = results.stream()
                .sorted( getSearchResultComparator( results, maxResults ) )
                .limit( maxResults )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
        topoSortTimer.stop();

        if ( timer.getTime() > ( isFullTextSupported ? 200 : 1000 ) ) {
            log.warn( String.format( "Found %d results (%d prior to sorting) suitable for autocompletion for query '%s' (normalized as %s, full-text as %s) in %d ms (initial query: %d ms, topological sort and ranking: %d ms).",
                    sortedResults.size(),
                    results.size(),
                    query,
                    normalizedQuery,
                    fullTextQuery,
                    timer.getTime(),
                    initialQueryTimer.getTime(),
                    topoSortTimer.getTime() ) );
        }

        return sortedResults;
    }

    private SearchResult<OntologyTermInfo> toSearchResult( OntologyTermInfo t, OntologyTermMatchType matchType, String extras, double tfIdf, Locale locale ) {
        SearchResult<OntologyTermInfo> result = new SearchResult<>(
                matchType,
                t.getId(),
                t.getTermId(),
                messageSource.getMessage( "rdp.ontologies." + t.getOntology().getName() + ".terms." + t.getName() + ".title", null, t.getName(), locale ),
                extras,
                t );
        result.setScore( tfIdf );
        return result;
    }

    @Secured("ROLE_ADMIN")
    public Ontology save( Ontology ontology ) {
        return ontologyRepository.save( ontology );
    }

    private Comparator<SearchResult<OntologyTermInfo>> getSearchResultComparator( List<SearchResult<OntologyTermInfo>> results, int maxResults ) {
        // collect the terms used for topological sorting
        Set<OntologyTermInfo> foundTerms = results.stream()
                .map( SearchResult::getMatch )
                .collect( Collectors.toSet() );
        // we get the similarity by match by picking the best match type
        Map<OntologyTermInfo, Double> similarityByMatch = results.stream()
                .sorted( Comparator.comparing( SearchResult<OntologyTermInfo>::getMatchType, MatchType.getComparator() ) )
                .collect( Collectors.toCollection( LinkedHashSet::new ) ).stream()
                .collect( Collectors.toMap( SearchResult::getMatch, SearchResult::getScore ) );
        return Comparator.comparing( SearchResult<OntologyTermInfo>::getMatchType, MatchType.getComparator() )
                .thenComparing( SearchResult::getMatch, getTopologicalComparator( foundTerms, similarityByMatch, maxResults ) );
    }

    public OntologyTermInfo findTermById( Integer ontologyTermId ) {
        return ontologyTermInfoRepository.findOne( ontologyTermId );
    }

    @Data
    private static class OntologyTermInfoWithDepth {
        private final OntologyTermInfo term;
        private final int depth;
    }

    /**
     * Create a comparator that results in a topological order for a set of terms.
     */
    private Comparator<OntologyTermInfo> getTopologicalComparator( Set<OntologyTermInfo> terms, Map<OntologyTermInfo, Double> similarityByTerm, int maxResults ) {
        StopWatch timer = StopWatch.createStarted();

        // this is the set of nodes to visit whose incoming edges have been removed
        // terms are visited by depth then by similarity to the initial query and then by term
        Queue<OntologyTermInfoWithDepth> S = new PriorityQueue<>( Comparator
                .comparing( OntologyTermInfoWithDepth::getDepth )
                // only terms that were found will be in this mapping, the remaining terms are merely used for finding a
                // topological order
                .thenComparing( otw -> similarityByTerm.getOrDefault( otw.getTerm(), 0.0 ), Comparator.reverseOrder() )
                .thenComparing( OntologyTermInfoWithDepth::getTerm ) );

        // collect all the ancestors of the terms, so we can prune unreachable paths
        // use ancestors without incident edges to initialize S which saves a phenomenal amount of time
        Set<OntologyTermInfo> A = new HashSet<>();
        Queue<OntologyTermInfo> fringe = new ArrayDeque<>( terms );
        while ( !fringe.isEmpty() ) {
            OntologyTermInfo node = fringe.remove();
            // already visited through another term
            if ( A.contains( node ) )
                continue;
            A.add( node );
            if ( node.getSuperTerms().isEmpty() ) {
                S.add( new OntologyTermInfoWithDepth( node, 0 ) );
            } else {
                fringe.addAll( node.getSuperTerms() );
            }
        }

        StringBuilder searchBreakdown = new StringBuilder();

        // collect nodes part of cycles
        Set<OntologyTermInfo> C = new HashSet<>();

        // this is the set of visited nodes, it allows us to "remove" edges by marking the corresponding node as visited
        Set<OntologyTermInfo> V = new HashSet<>();
        List<OntologyTermInfo> L = new ArrayList<>( terms.size() );
        while ( !S.isEmpty() ) {
            OntologyTermInfoWithDepth twd = S.remove();
            OntologyTermInfo n = twd.getTerm();

            if ( V.contains( n ) ) {
                C.add( n );
                continue;
            }

            if ( log.isDebugEnabled() ) {
                if ( twd.getDepth() > 0 )
                    searchBreakdown.append( '└' );
                for ( int i = 0; i < twd.depth - 1; i++ )
                    searchBreakdown.append( '─' );
                if ( twd.getDepth() > 0 ) {
                    searchBreakdown.append( ' ' );
                }
                if ( terms.contains( twd.getTerm() ) ) {
                    searchBreakdown.append( '*' ).append( ' ' );
                }
                searchBreakdown
                        .append( twd.getTerm() )
                        .append( '\n' );
            }

            // consider n visited, that's the equivalent of removing all outgoing edges
            V.add( n );

            // only add terms that were originally mentioned, otherwise we would be sorting the whole graph
            if ( terms.contains( n ) ) {
                L.add( n );

                if ( L.size() == maxResults || L.size() == terms.size() ) {
                    // finish early since we already sorted all the input terms or we've reached max results

                    // always a subset of the searched terms
                    assert terms.containsAll( L );

                    // if same size, must be identical
                    assert L.size() != terms.size() || new HashSet<>( L ).containsAll( terms );

                    if ( maxResults < terms.size() ) {
                        searchBreakdown
                                .append( '└' )
                                .append( ' ' )
                                .append( String.format( "There are %d remaining terms that weren't sorted since we reached maximum results (noted with *).", terms.size() - maxResults ) )
                                .append( '\n' );
                    }

                    break;
                }
            }

            for ( OntologyTermInfo m : n.getSubTerms() ) {
                // make sure that there's no incoming edges whose nodes were already visited
                // if all incoming edge nodes were already visited, add the node to be
                // also, we prune irrelevant sub terms that can't reach our set of terms via the set of ancestors A
                if ( A.contains( m ) && V.containsAll( m.getSuperTerms() ) ) {
                    S.add( new OntologyTermInfoWithDepth( m, twd.getDepth() + 1 ) );
                }
            }
        }

        // report cycles
        Map<Ontology, List<OntologyTermInfo>> r = C.stream().collect( Collectors.groupingBy( OntologyTermInfo::getOntology, Collectors.toList() ) );
        for ( Map.Entry<Ontology, List<OntologyTermInfo>> e : r.entrySet() ) {
            log.warn( String.format( "There are one or more cycles in the ontology %s as %s were already visited. The topological order will not be valid.",
                    e.getKey().getName(),
                    e.getValue().stream().sorted().distinct().map( OntologyTermInfo::getTermId ).collect( Collectors.joining( ", " ) ) ) );
        }

        // construct a O(1) indexOf-like access
        Map<OntologyTermInfo, Integer> Ls = new HashMap<>( L.size() );
        int i = 0;
        for ( OntologyTermInfo elem : L ) {
            Ls.put( elem, i++ );
        }

        timer.stop();

        String searchSummary = String.format( "Visited %d terms to sort %d terms in %d ms. There were %d remaining nodes to visit and %d ancestors used for pruning.",
                V.size(), L.size(), timer.getTime( TimeUnit.MILLISECONDS ),
                S.size(), A.size() );

        if ( timer.getTime( TimeUnit.MILLISECONDS ) > 200 ) {
            log.warn( searchSummary );
        } else {
            log.debug( searchSummary );
        }

        log.debug( "\n" + searchBreakdown );

        // don't bother comparing results that exceed the max results (all collapsed at the last virtual position)
        return Comparator.comparingInt( t -> Ls.getOrDefault( t, maxResults ) );
    }

    public OntologyTermInfo findByTermIdAndOntology( String ontologyTermInfoId, Ontology ontology ) {
        return ontologyTermInfoRepository.findByTermIdAndOntology( ontologyTermInfoId, ontology );
    }
}
