package ubc.pavlab.rdp.services;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
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

import javax.persistence.EntityManager;
import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.UnaryOperator.identity;

/**
 * This service combines both {@link OntologyTermInfoRepository} and {@link OntologyRepository}.
 *
 * @author poirgui
 */
@Service
@CommonsLog
public class OntologyService implements InitializingBean {

    /**
     * Maximum size of a "simple" ontology.
     * <p>
     * If you need to test if an ontology is deemed simple, consider using {@link #isSimple(Ontology)} instead.
     *
     * @see #isSimple(Ontology)
     */
    public static final int SIMPLE_ONTOLOGY_MAX_SIZE = 20;
    private static final DateFormat OBO_DATE_FORMAT = new SimpleDateFormat( "dd:MM:yyyy HH:mm" );

    private final OntologyRepository ontologyRepository;
    private final OntologyTermInfoRepository ontologyTermInfoRepository;

    @Autowired
    @Qualifier("messageSourceWithoutOntology")
    private MessageSource messageSource;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private BuildProperties buildProperties;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    public OntologyService( OntologyRepository ontologyRepository, OntologyTermInfoRepository ontologyTermInfoRepository ) {
        this.ontologyRepository = ontologyRepository;
        this.ontologyTermInfoRepository = ontologyTermInfoRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if ( isFullTextSupported() ) {
            log.info( "Full-text is supported by the database engine, it will be used for querying ontology terms efficiently." );
        } else {
            log.warn( "Full-text is not supported, ontology term matching will use the SQL LIKE syntax." );
        }
    }

    private boolean isFullTextSupported() {
        return HibernateUtils.getDialect( entityManager ) instanceof MySQLDialect;
    }

    public Ontology findById( Integer id ) {
        return ontologyRepository.findById( id ).orElse( null );
    }

    public Ontology findByName( String name ) {
        return ontologyRepository.findByName( name );
    }

    @Transactional(readOnly = true)
    public List<OntologyTermInfo> findAllTermsByIdIn( Collection<Integer> ontologyTermIds ) {
        return ontologyTermInfoRepository.findAllByActiveTrueAndIdIn( ontologyTermIds );
    }

    @Transactional(readOnly = true)
    public List<OntologyTermInfo> findAllTermsByIdInMaintainingOrder( List<Integer> ontologyTermIds ) {
        return ontologyTermInfoRepository.findAllById( ontologyTermIds ).stream()
                // FIXME: this is O(n^2)!
                .sorted( Comparator.comparing( oti -> ontologyTermIds.indexOf( oti.getId() ) ) )
                .collect( Collectors.toList() );
    }

    public List<Ontology> findAllOntologies() {
        return ontologyRepository.findAllByActiveTrue().stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    @Transactional(readOnly = true)
    public List<Ontology> findAllOntologiesAvailableForGeneSearch() {
        return ontologyRepository.findAllByActiveTrueAndAvailableForGeneSearchTrue().stream()
                .sorted( Ontology.getComparator() )
                .collect( Collectors.toList() );
    }

    @Transactional(readOnly = true)
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

    public Page<OntologyTermInfo> findAllTermsByOntologyIncludingInactive( Ontology ontology, Pageable pageable ) {
        return ontologyTermInfoRepository.findAllByOntology( ontology, pageable );
    }

    /**
     * Test for term equality in a loose sense.
     * <p>
     * Two terms are considered equal if they have the same ontology and term ID.
     * <p>
     * This is necessary because {@link UserOntologyTerm} and {@link OntologyTermInfo} do not have the same definition
     * of equality.
     */
    public boolean termEquals( OntologyTerm ontologyTerm, OntologyTerm other ) {
        // always compare termId first to avoid needlessly eager loading the ontology
        return ontologyTerm.getTermId().equals( other.getTermId() ) && ontologyTerm.getOntology().equals( other.getOntology() );
    }

    /**
     * Test if a term is contained in a given collection using the {@link #termEquals(OntologyTerm, OntologyTerm)}
     * definition for equality.
     */
    public boolean termIsIn( OntologyTerm term, Collection<? extends OntologyTerm> selectedTerms ) {
        return selectedTerms.stream().anyMatch( t -> termEquals( term, t ) );
    }

    /**
     * Restrict terms from an iterable to those contained in a given selected terms collection using {@link #termEquals(OntologyTerm, OntologyTerm)}
     * definition for equality.
     */
    public <T extends OntologyTerm> List<T> onlyTermsIn( Iterable<T> terms, Collection<? extends OntologyTerm> selectedTerms ) {
        List<T> results = new ArrayList<>();
        for ( T term : terms ) {
            if ( termIsIn( term, selectedTerms ) ) {
                results.add( term );
            }
        }
        return results;
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
        if ( ontologyName == null ) {
            throw new IllegalArgumentException( "Ontology has no defined name." );
        }
        if ( ontologyName.isEmpty() || ontologyName.length() > Ontology.MAX_NAME_LENGTH ) {
            throw new IllegalArgumentException( String.format( "Ontology name '%s' size must be between 1 and %d.", ontologyName, Ontology.MAX_NAME_LENGTH ) );
        }
        if ( ontologyRepository.existsByName( ontologyName ) ) {
            throw new OntologyNameAlreadyUsedException( ontologyName );
        }
        Ontology ontology = Ontology.builder( ontologyName ).build();
        return saveFromObo( ontology, parsingResult );
    }

    /**
     * Update an ontology from an OBO formatted input.
     * <p>
     * You might want to run {@link #propagateSubtreeActivation(Ontology)} afterward since newly added terms will be
     * inactive.
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

        Set<OntologyTermInfo> convertedTerms = new HashSet<>();

        // first pass for saving terms
        for ( OBOParser.Term term : parsingResult.getTerms() ) {
            assert term.getId() != null;
            assert term.getName() != null;
            OntologyTermInfo t = existingTermsById.get( term.getId() );
            if ( t == null ) {
                t = OntologyTermInfo.builder( ontology, term.getId() ).build();
            }

            if ( term.getName().length() > OntologyTermInfo.MAX_NAME_LENGTH ) {
                log.warn( String.format( "Name %s for term %s is too wide (%d characters) to fit in the database, it will be truncated to %d characters.",
                        term.getName(), term, term.getName().length(), OntologyTermInfo.MAX_NAME_LENGTH ) );
                t.setName( term.getName().substring( 0, OntologyTermInfo.MAX_NAME_LENGTH ) );
            } else {
                t.setName( term.getName() );
            }

            t.setOrdering( convertedTerms.size() + 1 );

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
                t.getSynonyms().add( s.getSynonym().toLowerCase() );
            }

            t.setObsolete( term.getObsolete() != null && term.getObsolete() );
            convertedTerms.add( t );
        }

        Map<String, OntologyTermInfo> convertedTermsById = convertedTerms.stream()
                .collect( Collectors.toMap( OntologyTermInfo::getTermId, identity() ) );

        // second pass for setting relationships
        for ( OBOParser.Term term : parsingResult.getTerms() ) {
            SortedSet<OntologyTermInfo> subTerms = term.getInverseRelationships().stream()
                    .filter( rel -> rel.getTypedef().equals( OBOParser.Typedef.IS_A ) )
                    .map( OBOParser.Term.Relationship::getNode )
                    .map( OBOParser.Term::getId )
                    .map( convertedTermsById::get )
                    .collect( Collectors.toCollection( TreeSet::new ) );
            convertedTermsById.get( term.getId() ).setSubTerms( subTerms );
        }

        CollectionUtils.update( ontology.getTerms(), convertedTerms );

        return ontologyRepository.save( ontology );
    }

    /**
     * Test if a given ontology is deemed simple.
     * <p>
     * This is measured with a size threshold - 20 at the moment - on the number of terms the ontology has regardless of
     * their active states.
     * <p>
     * The result of this method is cached, making it suitable for rendering without needlessly hitting the database to
     * get the number of terms.
     *
     * @see #SIMPLE_ONTOLOGY_MAX_SIZE
     */
    @Cacheable(value = "ubc.pavlab.rdp.services.OntologyService.simpleOntologies")
    public boolean isSimple( Ontology ontology ) {
        return ontology.getTerms().size() <= SIMPLE_ONTOLOGY_MAX_SIZE;
    }

    /**
     * Efficiently activate an ontology and all of its terms.
     * <p>
     * Obsolete terms are ignored.
     *
     * @param includeTerms indicate if terms in the ontology should be activated as well
     * @return the number of activated terms in the ontology, which is always zero if includeTerms is false
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public int activate( Ontology ontology, boolean includeTerms ) {
        ontologyRepository.activate( ontology );
        if ( includeTerms ) {
            return ontologyTermInfoRepository.activateByOntologyAndActiveFalseAndObsoleteFalse( ontology );
        } else {
            return 0;
        }
    }

    /**
     * Efficiently deactivate an ontology and all of its terms.
     * <p>
     * Unlike {@link #activate(Ontology, boolean)}, obsolete terms are included.
     *
     * @param ontology     the ontology to deactivate
     * @param includeTerms indicate if terms in the ontology should be deactivated as well
     * @return the number of deactivated terms, which is always zero if includeTerms is false
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public int deactivate( Ontology ontology, boolean includeTerms ) {
        ontologyRepository.deactivate( ontology );
        if ( includeTerms ) {
            return ontologyTermInfoRepository.deactivateByOntologyAndActiveFalse( ontology );
        } else {
            return 0;
        }
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void activateTerm( OntologyTermInfo ontologyTermInfo ) {
        ontologyTermInfo.setActive( true );
        ontologyTermInfoRepository.save( ontologyTermInfo );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void deactivateTerm( OntologyTermInfo ontologyTermInfo ) {
        ontologyTermInfo.setActive( false );
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
     * @param ontologyTermInfo the root of the subtree to activate where children are visited by {@link OntologyTermInfo#getSubTerms()}.
     * @return the number of activated terms
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public int activateTermSubtree( OntologyTermInfo ontologyTermInfo ) {
        return ontologyTermInfoRepository.activateByTermIdsAndActiveFalseAndObsoleteFalse( getDescendentIds( Collections.singleton( ontologyTermInfo ) ) );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public int deactivateTermSubtree( OntologyTermInfo ontologyTermInfo ) {
        return ontologyTermInfoRepository.deactivateByTermIdsAndActiveFalse( getDescendentIds( Collections.singleton( ontologyTermInfo ) ) );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    public void move( Ontology ontology, Direction direction ) {
        List<Ontology> ontologies = ontologyRepository.findAllByActiveTrue();
        ontologies.sort( Ontology.getComparator() );

        // compute the original position
        int i = ontologies.indexOf( ontology );

        if ( i == -1 ) {
            throw new IllegalArgumentException( "The provided ontology is not active." );
        }

        // compute the target position
        int destination = Math.min( Math.max( i + ( direction == Direction.UP ? -1 : 1 ), 0 ), ontologies.size() - 1 );

        // reinsert the ontology at the target position
        ontologies.remove( i );
        ontologies.add( destination, ontology );

        // reset ordering for everything
        int position = 0;
        for ( Ontology o : ontologies ) {
            o.setOrdering( position++ );
        }

        ontologyRepository.saveAll( ontologies );
    }

    /**
     * Find terms by term IDs and ontology names.
     * <p>
     * Terms that are duplicates or do not exist are ignored.
     *
     * @return the corresponding ontology terms excluding non-existing ones and duplicates
     */
    @Transactional(readOnly = true)
    public List<OntologyTermInfo> findTermByTermIdsAndOntologyNames( List<String> termIds, List<String> ontologyNames ) {
        if ( termIds.size() != ontologyNames.size() ) {
            throw new IllegalArgumentException( "Term IDs and ontology names list must have the same size." );
        }
        LinkedHashSet<OntologyTermInfo> results = new LinkedHashSet<>( termIds.size() );
        for ( int i = 0; i < ontologyNames.size(); i++ ) {
            OntologyTermInfo term = ontologyTermInfoRepository.findByTermIdAndOntologyName( termIds.get( i ), ontologyNames.get( i ) );
            if ( term != null ) {
                results.add( term );
            }
        }
        return new ArrayList<>( results );
    }

    public enum Direction {
        UP, DOWN
    }

    /**
     * Resolve the ontology name.
     * <p>
     * In templates, prefer instead {@link MessageSource} with the <code>rdp.ontologies.{ontologyName}.title</code> code.
     */
    public String resolveOntologyName( Ontology o, Locale locale ) {
        return messageSource.getMessage( "rdp.ontologies." + o.getName() + ".title", null, o.getName().toUpperCase(), locale );
    }

    /**
     * Resolve the ontology definition.
     * <p>
     * In templates, prefer instead {@link MessageSource} with the <code>rdp.ontologies.{ontologyName}.definition</code>
     * code.
     */
    public String resolveOntologyTermInfoName( OntologyTermInfo t, Locale locale ) {
        return messageSource.getMessage( "rdp.ontologies." + t.getOntology().getName() + ".terms." + t.getName() + ".title", null, t.getName(), locale );
    }

    /**
     * Resolve an ontology URL into a {@link Resource}.
     */
    public Resource resolveOntologyUrl( URL ontologyUrl ) {
        return resourceLoader.getResource( ontologyUrl.toString() );
    }

    @Transactional
    public void updateOntologies() {
        log.info( "Updating all active ontologies..." );
        for ( Ontology ontology : findAllOntologies() ) {
            if ( ontology.getOntologyUrl() != null ) {
                Resource resource = resolveOntologyUrl( ontology.getOntologyUrl() );
                log.info( "Updating " + ontology + " from " + resource + "..." );
                try ( Reader reader = new InputStreamReader( resource.getInputStream() ) ) {
                    updateFromObo( ontology, reader );
                    log.info( "Updated " + ontology + " from " + resource + "." );
                    int numActivated = propagateSubtreeActivation( ontology );
                    if ( numActivated > 0 ) {
                        log.info( String.format( "%d terms got activated via subtree propagation in %s.", numActivated, ontology ) );
                    }
                } catch ( FileNotFoundException e ) {
                    log.warn( String.format( "The update of %s will be skipped: %s does not exist.", ontology, resource ) );
                } catch ( IOException | ParseException e ) {
                    log.error( "Failed to update " + ontology + ".", e );
                }
            }
        }
        log.info( "Ontologies have been successfully updated." );
    }

    /**
     * Write the provided ontology to OBO format.
     * <p>
     * TODO: how should inactive terms be handled?
     */
    @Transactional(readOnly = true)
    public void writeObo( Ontology ontology, Writer writer ) throws IOException {
        try ( OboWriter bw = new OboWriter( writer ) ) {
            bw.writeLine( "format-version: 1.2" );
            bw.writeLine( "ontology: " + ontology.getName() );
            bw.writeLine( "date: " + OBO_DATE_FORMAT.format( Date.from( Instant.now() ) ) );
            bw.writeLine( String.format( "auto-generated-by: %s:%s %s", buildProperties.getGroup(),
                    buildProperties.getArtifact(), buildProperties.getVersion() ) );
            boolean hasOrderedTerms = false, hasGroupingTerms = false;
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
                    hasOrderedTerms = true;
                    bw.writeLine( String.format( "relationship: has_order %s {order=\"%d\"}", term.getTermId(), term.getOrdering() ) );
                }
                if ( term.isGroup() ) {
                    hasGroupingTerms = true;
                    bw.writeLine( String.format( "relationship: is_grouping %s", term.getTermId() ) );
                }
            }
            if ( hasOrderedTerms ) {
                bw.writeTypedef( "has_order", "Term has relative order to other terms defined at the same subclass level." );
            }
            if ( hasGroupingTerms ) {
                bw.writeTypedef( "is_grouping", "Indicate that the term is merely grouping other terms." );
            }
        }
    }


    @Transactional
    @Secured("ROLE_ADMIN")
    public Ontology create( Ontology ontology ) throws OntologyNameAlreadyUsedException {
        if ( ontology.getId() != null ) {
            throw new IllegalArgumentException( String.format( "Ontology %s already exists, it cannot be created.", ontology.getName() ) );
        }
        if ( ontologyRepository.existsByName( ontology.getName() ) ) {
            throw new OntologyNameAlreadyUsedException( ontology.getName() );
        }
        return ontologyRepository.save( ontology );
    }

    @Transactional
    @Secured("ROLE_ADMIN")
    public void updateNameAndTerms( Ontology ontology, String name, Set<OntologyTermInfo> terms ) throws OntologyNameAlreadyUsedException {
        if ( ontology.getId() == null ) {
            throw new IllegalArgumentException( String.format( "Ontology %s does not exist, it cannot be updated.", ontology.getName() ) );
        }
        if ( !ontology.getName().equals( name ) && ontologyRepository.existsByName( name ) ) {
            throw new OntologyNameAlreadyUsedException( "Ontology already exists." );
        }
        ontology.setName( name );
        CollectionUtils.update( ontology.getTerms(), terms );
        ontologyRepository.save( ontology );
    }

    @Transactional
    public void saveTermsNoAuth( Iterable<OntologyTermInfo> terms ) {
        ontologyTermInfoRepository.saveAll( terms );
    }

    @Transactional(readOnly = true)
    public OntologyTermInfo findTermByTermIdAndOntologyName( String termId, String ontologyName ) {
        return ontologyTermInfoRepository.findByTermIdAndOntologyName( termId, ontologyName );
    }

    @Transactional
    public Ontology saveNoAuth( Ontology ontology ) {
        return ontologyRepository.save( ontology );
    }

    @Transactional(readOnly = true)
    public String findDefinitionByOntologyName( String ontologyName ) {
        return ontologyTermInfoRepository.findDefinitionByOntologyName( ontologyName );
    }

    @Transactional(readOnly = true)
    public String findDefinitionByTermNameAndOntologyName( String termName, String ontologyName ) {
        return ontologyTermInfoRepository.findAllDefinitionsByNameAndOntologyName( termName, ontologyName ).stream()
                .findFirst()
                .orElse( null );
    }

    private static class OboWriter extends BufferedWriter {

        public OboWriter( Writer out ) {
            super( out );
        }

        public void writeTypedef( String id, String definition ) throws IOException {
            newLine();
            writeLine( "[Typedef]" );
            writeLine( String.format( "id: %s", id ) );
            writeLine( "def: " + '"' + definition + '"' + " []" );
            writeLine( "is_reflexive: true" );
            writeLine( "is_metadata_tag: true" );
        }

        public void writeLine( String s ) throws IOException {
            write( s );
            newLine();
        }
    }

    /**
     * Delete an ontology.
     * <p>
     * Important note: this will fail if any user has an {@link UserOntologyTerm} associated to the supplied ontology.
     * There is no implicit cascading, so you must delete the user terms first.
     *
     * @throws DataIntegrityViolationException generally if a user still has a term associated to that ontology, but
     *                                         it could because by something else
     */
    @Secured("ROLE_ADMIN")
    @Transactional
    public void delete( Ontology ontology ) throws DataIntegrityViolationException {
        ontologyRepository.delete( ontology );
        ontologyRepository.flush(); // necessary if we want to raise the DataIntegrityViolationException
    }

    /**
     * Autocomplete terms from a given ontology.
     */
    @Transactional(readOnly = true)
    public List<SearchResult<OntologyTermInfo>> autocompleteTerms( String query, Ontology ontology, int maxResults, Locale locale ) {
        return autocompleteTerms( query, Collections.singleton( ontology ), true, maxResults, locale );
    }

    /**
     * Autocomplete terms from all active ontologies.
     */
    @Transactional(readOnly = true)
    public List<SearchResult<OntologyTermInfo>> autocompleteTerms( String query, int maxResults, Locale locale ) {
        return autocompleteTerms( query, new HashSet<>( ontologyRepository.findAllByActiveTrue() ), true, maxResults, locale );
    }

    /**
     * Autocomplete inactive terms form a given ontology.
     * <p>
     * This is meant for the administrative section.
     */
    @Secured("ROLE_ADMIN")
    @Transactional(readOnly = true)
    public List<SearchResult<OntologyTermInfo>> autocompleteInactiveTerms( String query, Ontology ontology, int maxResults, Locale locale ) {
        return autocompleteTerms( query, Collections.singleton( ontology ), false, maxResults, locale );
    }

    private List<SearchResult<OntologyTermInfo>> autocompleteTerms( String query, Set<Ontology> ontologies, boolean active, int maxResults, Locale locale ) {
        List<SearchResult<OntologyTermInfo>> results = new ArrayList<>( maxResults );

        String normalizedQuery = TextUtils.normalize( query );

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
        ontologyTermInfoRepository.findAllByOntologyInAndTermIdIgnoreCaseAndActive( ontologies, normalizedQuery, active ).stream()
                .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_ID_EXACT, null, 1.0, locale ) )
                .forEachOrdered( results::add );

        // alt IDs
        ontologyTermInfoRepository.findAllByOntologyInAndAltTermIdsContainingIgnoreCaseAndActive( ontologies, normalizedQuery, active ).stream()
                .map( t1 -> toSearchResult( t1, OntologyTermMatchType.ALT_ID_EXACT, String.join( ", ", t1.getAltTermIds() ), 1.0, locale ) )
                .forEachOrdered( results::add );

        // term name
        if ( results.size() < maxResults ) {

            // then some occurrences in the term name
            // by full text (if supported)
            if ( isFullTextSupported() ) {
                ontologyTermInfoRepository.findAllByOntologyInAndNameMatchAndActive( ontologies, fullTextQuery, active ).stream()
                        .map( row -> Pair.of( (OntologyTermInfo) row[0], (Double) row[1] ) )
                        .map( t1 -> toSearchResult( t1.getFirst(), OntologyTermMatchType.TERM_NAME_MATCH, null, t1.getSecond() / TextUtils.tokenize( t1.getFirst().getName() ).size(), locale ) )
                        .forEachOrdered( results::add );
            } else {
                ontologyTermInfoRepository.findAllByOntologyInAndNameIgnoreCaseAndActive( ontologies, normalizedQuery, active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_EXACT, null, 0.0, locale ) )
                        .forEachOrdered( results::add );
                ontologyTermInfoRepository.findAllByOntologyInAndNameLikeIgnoreCaseAndActive( ontologies, normalizedQuery + "%", active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_STARTS_WITH, null, 0.0, locale ) )
                        .forEachOrdered( results::add );
                ontologyTermInfoRepository.findAllByOntologyInAndNameLikeIgnoreCaseAndActive( ontologies, "%" + normalizedQuery + "%", active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.TERM_NAME_CONTAINS, null, 0.0, locale ) )
                        .forEachOrdered( results::add );
            }
        }

        // then by synonyms
        if ( results.size() < maxResults ) {
            // full text (if available)
            if ( isFullTextSupported() ) {
                ontologyTermInfoRepository.findAllByOntologyInAndSynonymsMatchAndActive( ontologies, fullTextQuery, active ).stream()
                        .map( row -> toSearchResult( (OntologyTermInfo) row[0], OntologyTermMatchType.SYNONYM_MATCH, (String) row[1], (Double) row[2], locale ) )
                        .forEachOrdered( results::add );
            } else {
                ontologyTermInfoRepository.findAllByOntologyInAndSynonymsContainingIgnoreCaseAndActive( ontologies, normalizedQuery, active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_EXACT, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .forEachOrdered( results::add );
                ontologyTermInfoRepository.findAllByOntologyInAndSynonymsLikeIgnoreCaseAndActive( ontologies, normalizedQuery + "%", active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_STARTS_WITH, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .forEachOrdered( results::add );
                ontologyTermInfoRepository.findAllByOntologyInAndSynonymsLikeIgnoreCaseAndActive( ontologies, "%" + normalizedQuery + "%", active ).stream()
                        .map( t1 -> toSearchResult( t1, OntologyTermMatchType.SYNONYM_CONTAINS, String.join( ", ", t1.getSynonyms() ), 0.0, locale ) )
                        .forEachOrdered( results::add );
            }
        }

        // then some definitions
        if ( results.size() < maxResults ) {
            if ( isFullTextSupported() ) {
                ontologyTermInfoRepository.findAllByOntologyInAndDefinitionMatchAndActive( ontologies, fullTextQuery, active ).stream()
                        .map( row -> Pair.of( (OntologyTermInfo) row[0], (Double) row[1] ) )
                        .map( t -> toSearchResult( t.getFirst(), OntologyTermMatchType.DEFINITION_MATCH, t.getFirst().getDefinition(), t.getSecond(), locale ) )
                        .forEachOrdered( results::add );

            } else {
                ontologyTermInfoRepository.findAllByOntologyInAndDefinitionLikeIgnoreCaseAndActive( ontologies, "%" + normalizedQuery + "%", active ).stream()
                        .map( t -> toSearchResult( t, OntologyTermMatchType.DEFINITION_CONTAINS, t.getDefinition(), 0.0, locale ) )
                        .forEachOrdered( results::add );
            }
        }

        initialQueryTimer.stop();

        StopWatch topologicalSortTimer = StopWatch.createStarted();
        Collection<SearchResult<OntologyTermInfo>> sortedResults = results.stream()
                .sorted( getSearchResultComparator( results, maxResults ) )
                .limit( maxResults )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
        topologicalSortTimer.stop();

        String searchSummary = String.format( "Found %d suitable results for autocompletion of query '%s' (normalized as '%s', full-text as '%s') in %d ms (initial query: %d ms, topological sort and ranking: %d ms).",
                results.size(), query, normalizedQuery, fullTextQuery, timer.getTime(), initialQueryTimer.getTime(), topologicalSortTimer.getTime() );

        if ( timer.getTime() > ( isFullTextSupported() ? 500 : 1000 ) ) {
            log.warn( searchSummary );
        } else {
            log.debug( searchSummary );
        }

        return new ArrayList<>( sortedResults );
    }

    /**
     * Infer a set of terms numerical IDs meant by a collection of ontology terms.
     * <p>
     * Note: the returned terms are not filtered for their active state as it would be too inefficient to do this here
     * without loading the terms.
     * <p>
     * Caching makes most sense for top terms which are more expensive to infer due to the large number of descendents.
     *
     * @return inferred term IDs
     */
    @Transactional(readOnly = true)
    public Set<Integer> inferTermIds( Collection<OntologyTermInfo> ontologyTermInfos ) {
        StopWatch timer = StopWatch.createStarted();
        try {
            return getDescendentIds( ontologyTermInfos );
        } finally {
            if ( timer.getTime( TimeUnit.MILLISECONDS ) > 500 ) {
                log.warn( String.format( "Inference took %d ms for the following terms: %s.",
                        timer.getTime( TimeUnit.MILLISECONDS ),
                        ontologyTermInfos.stream().map( OntologyTermInfo::toString ).collect( Collectors.joining( ", " ) ) ) );
            }
        }
    }

    /**
     * Perform the same inference as per {@link #inferTermIds(Collection)} with results grouped by ontology.
     */
    @Transactional(readOnly = true)
    public Map<Ontology, Set<Integer>> inferTermIdsByOntology( Collection<OntologyTermInfo> ontologyTermInfos ) {
        return ontologyTermInfos.stream()
                .collect( Collectors.groupingBy( OntologyTerm::getOntology,
                        Collectors.collectingAndThen( Collectors.toSet(), this::inferTermIds ) ) );
    }

    /**
     * Calculate the size of the subtree rooted in the given term.
     * <p>
     * Note: the result of this function is cached for performance reason, if you need to get an accurate subtree size,
     * consider using the size of {@link #inferTermIds(Collection)} instead.
     *
     * @return the subtree size, which is at least one
     */
    @Cacheable(value = "ubc.pavlab.rdp.services.OntologyService.subtreeSizeByTerm")
    public long subtreeSize( OntologyTermInfo term ) {
        return inferTermIds( Collections.singleton( term ) ).size();
    }

    private SearchResult<OntologyTermInfo> toSearchResult( OntologyTermInfo t, OntologyTermMatchType matchType, String extras, double tfIdf, Locale locale ) {
        SearchResult<OntologyTermInfo> result = new SearchResult<>(
                matchType,
                t.getId(),
                t.getTermId(),
                resolveOntologyTermInfoName( t, locale ),
                t );
        result.setExtras( extras );
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
        return ontologyTermInfoRepository.findById( ontologyTermId ).orElse( null );
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
                        .append( String.format( " [%d ms]", timer.getTime( TimeUnit.MILLISECONDS ) ) )
                        .append( '\n' );
            }

            // consider n visited, that's the equivalent of removing all outgoing edges
            V.add( n );

            // only add terms that were originally mentioned, otherwise we would be sorting the whole graph
            if ( terms.contains( n ) ) {
                L.add( n );

                if ( L.size() == maxResults || L.size() == terms.size() ) {
                    // finish early since we already sorted all the input terms, or we've reached max results

                    // always a subset of the searched terms
                    assert terms.containsAll( L );

                    // if same size, must be identical
                    assert L.size() != terms.size() || new HashSet<>( L ).containsAll( terms );

                    if ( maxResults < terms.size() ) {
                        searchBreakdown
                                .append( '└' )
                                .append( ' ' )
                                .append( String.format( "There are %d remaining terms that weren't sorted since we reached maximum number of results (matches are noted with *).", terms.size() - maxResults ) )
                                .append( String.format( " [%d ms]", timer.getTime( TimeUnit.MILLISECONDS ) ) )
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
            log.warn( searchSummary + String.format( " Set %s logger to DEBUG to view the full search breakdown.", OntologyService.class.getName() ) );
        } else {
            log.debug( searchSummary );
        }

        log.debug( "\n" + searchBreakdown );

        // don't bother comparing results that exceed the max results (all collapsed at the last virtual position)
        return Comparator.comparingInt( t -> Ls.getOrDefault( t, maxResults ) );
    }

    @Transactional(readOnly = true)
    public OntologyTermInfo findTermByTermIdAndOntology( String ontologyTermInfoId, Ontology ontology ) {
        return ontologyTermInfoRepository.findByTermIdAndOntology( ontologyTermInfoId, ontology );
    }

    @Transactional(readOnly = true)
    public boolean existsByTermIdAndOntologyAndActiveTrue( String termId, Ontology ontology ) {
        return ontologyTermInfoRepository.existsByTermIdAndOntologyAndActiveTrue( termId, ontology );
    }

    /**
     * Indicate if the ontology has active terms with icons.
     */
    @Transactional(readOnly = true)
    public boolean hasIcons( Ontology ontology ) {
        return ontologyTermInfoRepository.countByOntologyAndActiveTrueAndHasIconTrue( ontology ) > 0;
    }

    /**
     * Count the number of active terms in a given ontology.
     */
    @Transactional(readOnly = true)
    public long countActiveTerms( Ontology ontology ) {
        return ontologyTermInfoRepository.countByOntologyAndActiveTrue( ontology );
    }

    /**
     * Count the number of obsolete terms in a given ontology.
     */
    @Transactional(readOnly = true)
    public long countObsoleteTerms( Ontology ontology ) {
        return ontologyTermInfoRepository.countByOntologyAndObsoleteTrue( ontology );
    }

    @Transactional(readOnly = true)
    public long countActiveAndObsoleteTerms( Ontology ontology ) {
        return ontologyTermInfoRepository.countByOntologyAndActiveTrueAndObsoleteTrue( ontology );
    }

    /**
     * Find all active, non-trivial subtrees.
     * <p>
     * Those are active terms whose super terms are not active and have at least one sub term. Note that active terms
     * without parents are deemed to form active subtrees.
     * <p>
     * Also note that it is not guaranteed that all the descendents of the returned terms are active.
     */
    public List<OntologyTermInfo> findAllActiveNonTrivialSubtrees( Ontology ontology ) {
        return ontologyTermInfoRepository.findAllByOntologyAndActiveAndSuperTermsEmptyAndSubTermsNotEmpty( ontology );
    }

    /**
     * Obtain the IDs of the descendent terms.
     */
    private Set<Integer> getDescendentIds( Collection<OntologyTermInfo> terms ) {
        Set<Integer> fringe = terms.stream()
                .map( OntologyTermInfo::getId )
                .collect( Collectors.toSet() );
        Set<Integer> V = new HashSet<>();
        while ( !fringe.isEmpty() ) {
            List<Integer> subTermIds = ontologyTermInfoRepository.findSubTermsIdsByTermIdIn( fringe );
            V.addAll( fringe );
            // remove already visited nodes
            subTermIds.removeIf( V::contains );
            // set the fringe
            fringe.clear();
            fringe.addAll( subTermIds );
        }
        return V;
    }

    /**
     * Propagate subtree activation to ensure that subtrees are fully activated in a given ontology.
     * <p>
     * Inactive terms within an active subtree can occur if it was recently added or manually inactivated. We don't
     * allow that level of granularity, so this routine will propagate the activation to the whole subtree.
     * <p>
     * Obsolete terms are ignored.
     */
    @Transactional
    public int propagateSubtreeActivation( Ontology ontology ) {
        List<OntologyTermInfo> subtrees = ontologyTermInfoRepository.findAllByOntologyAndActiveAndSuperTermsEmpty( ontology );
        Set<Integer> descendants = getDescendentIds( subtrees );
        if ( descendants.isEmpty() ) {
            return 0;
        } else {
            return ontologyTermInfoRepository.activateByTermIdsAndActiveFalseAndObsoleteFalse( descendants );
        }
    }
}
