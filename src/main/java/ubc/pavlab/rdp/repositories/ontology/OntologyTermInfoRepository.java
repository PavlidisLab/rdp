package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.util.OBOParser;

import javax.persistence.QueryHint;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * We generally try to be careful here because a lot of terms can be retrieved.
 *
 * @author poirigui
 */
@Repository
public interface OntologyTermInfoRepository extends JpaRepository<OntologyTermInfo, Integer> {

    OntologyTermInfo findByTermIdAndOntology( String ontologyTermInfoId, Ontology ontology );

    /**
     * Retrieve all active terms in all ontologies in a consumable stream.
     */
    Stream<OntologyTermInfo> findAllByActiveTrueAndOntologyActiveTrue();

    List<OntologyTermInfo> findAllByActiveTrueAndIdIn( Collection<Integer> ids );

    /**
     * Retrieve all active terms in a given ontology in a paginated format.
     * <p>
     * Some ontologies (i.e. MONDO, UBERON) have thousands of terms with substantial definition text that can take
     * significant transport time, which is why we favour pagination.
     */
    Page<OntologyTermInfo> findAllByActiveTrueAndOntology( Ontology ontology, Pageable pageable );

    /**
     * Retrieve all terms in a paginated format.
     */
    Page<OntologyTermInfo> findAllByOntology( Ontology ontology, Pageable pageable );

    List<OntologyTermInfo> findAllByActiveTrueAndNameAndOntologyName( String name, String ontologyName );

    /**
     * Retrieve all active terms from active ontologies with the given term ID.
     * <p>
     * In general, this corresponds to only one term, but our system does not prevent multiple ontologies from sharing
     * terms.
     */
    List<OntologyTermInfo> findAllByOntologyInAndTermIdIgnoreCaseAndActive( Set<Ontology> ontologies, String query, boolean active );

    @Query("select t from OntologyTermInfo t join t.altTermIds a where t.active = :active and t.ontology in :ontologies and upper(a) = upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndAltTermIdsContainingIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    List<OntologyTermInfo> findAllByOntologyInAndNameIgnoreCaseAndActive( Set<Ontology> ontologies, String query, boolean active );

    /**
     * Retrieve all active terms from active ontologies whose term name that match the given pattern.
     */
    List<OntologyTermInfo> findAllByOntologyInAndNameLikeIgnoreCaseAndActive( Set<Ontology> ontologies, String pattern, boolean active );

    /**
     * Retrieve all ontology term info.
     * <p>
     * This will only work on the MySQL vendor.
     *
     * @return a list two elements arrays where the first is the {@link OntologyTermInfo} ID and second its full text
     * score against the query
     */
    @Query(value = "select t, match(t.name, :query) as score from OntologyTermInfo t where t.active = :active and t.ontology in :ontologies and match(t.name, :query) > 0")
    List<Object[]> findAllByOntologyInAndNameMatchAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = :active and t.ontology in :ontologies and upper(s) = upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndSynonymsContainingIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = :active and t.ontology in :ontologies and upper(s) like upper(:query)")
    List<OntologyTermInfo> findAllByOntologyInAndSynonymsLikeIgnoreCaseAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    /**
     * Note: the query is lower-cased in SQL to match synonyms' collation. See relevant
     *
     * @return triplets of {@link OntologyTermInfo}, matched synonym as {@link String} and full-text score as {@link Double}
     * @see ubc.pavlab.rdp.services.OntologyService#createFromObo(Reader)
     * @see ubc.pavlab.rdp.services.OntologyService#updateFromObo(Ontology, Reader)
     */
    @Query(value = "select t, synonym, match(synonym, lower(:query)) as score from OntologyTermInfo t join t.synonyms as synonym where t.active = :active and t.ontology in :ontologies and match(synonym, lower(:query)) > 0")
    List<Object[]> findAllByOntologyInAndSynonymsMatchAndActive( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query, @Param("active") boolean active );

    /**
     * Retrieve all active terms from active ontologies whose definition match the given pattern.
     */
    List<OntologyTermInfo> findAllByOntologyInAndDefinitionLikeIgnoreCaseAndActive( Set<Ontology> ontologies, String pattern, boolean active );

    @Query(value = "select t, match(t.definition, :query) as score from OntologyTermInfo t where t.active = :active and t.ontology in :ontologies and match(t.definition, :query) > 0")
    List<Object[]> findAllByOntologyInAndDefinitionMatchAndActive( @Param("ontologies") Set<Ontology> ontologyIds, @Param("query") String query, @Param("active") boolean active );

    /**
     * Count the number of active terms.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByActiveTrue();

    /**
     * Count the number of active terms in a given ontology.
     * <p>
     * Note: the result if this query is cached, so it should not be relied upon.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndActiveTrue( Ontology ontology );

    /**
     * Count the number of active obsolete terms in a given ontology.
     * <p>
     * Note: the result if this query is cached, so it should not be relied upon.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndActiveTrueAndObsoleteTrue( Ontology ontology );

    /**
     * Count the number of obsolete terms in a given ontology.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndObsoleteTrue( Ontology ontology );

    /**
     * Count the number of active terms with icons in a given ontology.
     * <p>
     * Note: this method does not check if the passed ontology is active or not.
     *
     * @return the number of terms with icons in the ontology
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    long countByOntologyAndActiveTrueAndHasIconTrue( Ontology ontology );

    /**
     * Activate all the terms in a given ontology.
     * <p>
     * Already active or obsolete terms are ignored.
     *
     * @return the number of activated terms in the ontology
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = true where t.ontology = :ontology and t.active = false and t.obsolete = false")
    int activateByOntologyAndActiveFalseAndObsoleteFalse( @Param("ontology") Ontology ontology );

    /**
     * Deactivate all the terms in a given ontology.
     * <p>
     * Already inactive terms are ignored. Unlike {@link #activateByOntologyAndActiveFalseAndObsoleteFalse(Ontology)},
     * obsolete terms are not ignored.
     *
     * @return the number of deactivated terms in the ontology
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = false where t.ontology = :ontology and t.active = true")
    int deactivateByOntologyAndActiveFalse( @Param("ontology") Ontology ontology );

    /**
     * Already active or obsolete terms are ignored.
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = true where t.id in :termIds and t.active = false and t.obsolete= false")
    int activateByTermIdsAndActiveFalseAndObsoleteFalse( @Param("termIds") Set<Integer> termIds );

    /**
     * Deactivate terms by IDs.
     * <p>
     * Unlike {@link #activateByTermIdsAndActiveFalseAndObsoleteFalse(Set)}, obsolete terms will be deactivated.
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = false where t.id in :termIds and t.active = true")
    int deactivateByTermIdsAndActiveFalse( @Param("termIds") Set<Integer> termIds );

    /**
     * Quickly gather subterm IDs.
     */
    @Query(value = "select ontology_sub_term_info_id from ontology_term_info_sub_terms where ontology_term_info_id in :termIds", nativeQuery = true)
    List<Integer> findSubTermsIdsByTermIdIn( @Param("termIds") Set<Integer> termIds );

    OntologyTermInfo findByTermIdAndOntologyName( String termId, String ontologyName );

    /**
     * Retrieve all the definitions matching an term name and its corresponding ontology name.
     * <p>
     * TODO: make it so that the results are ranked by desirableness
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select t.definition from OntologyTermInfo t where t.name = :termName and t.ontology.name = :ontologyName")
    List<String> findAllDefinitionsByNameAndOntologyName( @Param("termName") String termName, @Param("ontologyName") String ontologyName );

    /**
     * Find all terms whose super terms are not active in a given ontology.
     * <p>
     * Under the assumption that all the descendents of an active terms are also active, this is equivalent to an active
     * subtree.
     * <p>
     * This also includes terms that have no parents (i.e. root terms).
     */
    @Query("select t from OntologyTermInfo t left join t.superTerms st on st.active = true where t.ontology = :ontology and t.active = true group by t having count(st) = 0")
    List<OntologyTermInfo> findAllByOntologyAndActiveAndSuperTermsEmpty( @Param("ontology") Ontology ontology );

    /**
     * Like {@link #findAllByOntologyAndActiveAndSuperTermsEmpty(Ontology)}, but excluding terms that have no sub terms.
     *
     * @see #findAllByOntologyAndActiveAndSuperTermsEmpty(Ontology)
     */
    @Query("select t from OntologyTermInfo t left join t.superTerms st on st.active = true where t.ontology = :ontology and t.active = true and size(t.subTerms) > 0 group by t having count(st) = 0")
    List<OntologyTermInfo> findAllByOntologyAndActiveAndSuperTermsEmptyAndSubTermsNotEmpty( @Param("ontology") Ontology ontology );

    boolean existsByTermIdAndOntologyAndActiveTrue( String termId, Ontology ontology );
}
