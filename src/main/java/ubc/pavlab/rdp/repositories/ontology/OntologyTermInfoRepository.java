package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * We generally try to be careful here because a lot of terms can be retrieved.
 *
 * @author poirigui
 */
@Repository
public interface OntologyTermInfoRepository extends JpaRepository<OntologyTermInfo, Integer> {

    OntologyTermInfo findByTermIdAndOntology( String ontologyTermInfoId, Ontology ontology );

    OntologyTermInfo findByTermIdAndOntologyId( String termId, Integer ontologyId );

    /**
     * Retrieve all active terms in all ontologies in a consumable stream.
     */
    Stream<OntologyTermInfo> findAllByActiveTrueAndOntologyActiveTrue();

    /**
     * Retrieve all active terms in a given ontology in a paginated format.
     * <p>
     * Some ontologies (i.e. MONDO, UBERON) have thousands of terms with substantial definition text that can take
     * significant transport time, which is why we favour pagination.
     */
    Page<OntologyTermInfo> findAllByActiveTrueAndOntology( Ontology ontology, Pageable pageable );

    /**
     * Retrieve all active terms from active ontologies with the given term ID.
     * <p>
     * In general, this corresponds to only one term, but our system does not prevent multiple ontologies from sharing
     * terms.
     */
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndTermIdIgnoreCase( Set<Ontology> ontologies, String query );

    @Query("select t from OntologyTermInfo t join t.altTermIds a where t.active = true and t.ontology in :ontologies and upper(a) = upper(:query)")
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndAltTermIdsContainingIgnoreCase( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query );

    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndNameIgnoreCase( Set<Ontology> ontologies, String query );

    /**
     * Retrieve all active terms from active ontologies whose term name that match the given pattern.
     */
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndNameLikeIgnoreCase( Set<Ontology> ontologies, String pattern );

    /**
     * Retrieve all ontology term info.
     * <p>
     * This will only work on the MySQL vendor.
     *
     * @return a list two elements arrays where the first is the {@link OntologyTermInfo} ID and second its full text
     * score against the query
     */
    @Query(value = "select t.ontology_term_info_id, match(t.name) against(:query in boolean mode) as score from ontology_term_info t join ontology o on t.ontology_id = o.ontology_id where t.active and o.ontology_id in (:ontologyIds) having score > 0", nativeQuery = true)
    List<Object[]> findAllByActiveTrueAndOntologyInAndNameMatch( @Param("ontologyIds") Set<Integer> ontologyIds, @Param("query") String query );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = true and t.ontology in :ontologies and upper(s) = upper(:query)")
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndSynonymsContainingIgnoreCase( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query );

    @Query("select t from OntologyTermInfo t join t.synonyms s where t.active = true and t.ontology in :ontologies and upper(s) like upper(:query)")
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndSynonymsLikeIgnoreCase( @Param("ontologies") Set<Ontology> ontologies, @Param("query") String query );

    @Query(value = "select t.ontology_term_info_id, match(otis.synonym) against (:query in boolean mode) as score from ontology_term_info t join ontology o on o.ontology_id = t.ontology_id join ontology_term_info_synonyms otis on t.ontology_term_info_id = otis.ontology_term_info_id where t.active and o.ontology_id in :ontologyIds having score > 0", nativeQuery = true)
    List<Object[]> findAllByActiveTrueAndOntologyInAndSynonymsMatch( @Param("ontologyIds") Set<Ontology> ontologyIds, @Param("query") String query );

    /**
     * Retrieve all active terms from active ontologies whose definition match the given pattern.
     */
    List<OntologyTermInfo> findAllByActiveTrueAndOntologyInAndDefinitionLikeIgnoreCase( Set<Ontology> ontologies, String pattern );

    @Query(value = "select t.ontology_term_info_id, match(t.definition) against (:query in boolean mode) as score from ontology_term_info t join ontology o on o.ontology_id = t.ontology_id where t.active and o.ontology_id in :ontologyIds having score > 0", nativeQuery = true)
    List<Object[]> findAllByActiveTrueAndOntologyInAndDefinitionMatch( @Param("ontologyIds") Set<Integer> ontologyIds, @Param("query") String query );

    /**
     * Count the number of active terms.
     */
    long countByActiveTrue();

    /**
     * Activate all the terms in a given ontology.
     * <p>
     * Obsolete terms are ignored.
     *
     * @return the number of activated terms in the ontology
     */
    @Modifying
    @Query("update OntologyTermInfo t set t.active = true where t.ontology = :ontology and t.obsolete = false")
    int activateByOntologyAndObsoleteFalse( @Param("ontology") Ontology ontology );

    @Modifying
    @Query("update OntologyTermInfo  t set t.active = true where t.id in :termIds and t.obsolete= false")
    int activateByTermIdsAndObsoleteFalse( @Param("termIds") Set<Integer> termIds );

    /**
     * Quickly gather subterm IDs.
     */
    @Query(value = "select ontology_sub_term_info_id from ontology_term_info_sub_terms where ontology_term_info_id in :termIds", nativeQuery = true)
    List<Integer> findSubTermsIdsByTermIdIn( @Param("termIds") Set<Integer> termIds );
}
