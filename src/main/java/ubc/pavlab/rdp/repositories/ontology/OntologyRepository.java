package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;

import java.util.List;
import java.util.Optional;

/**
 * @author poirigui
 */
@Repository
public interface OntologyRepository extends JpaRepository<Ontology, Integer> {

    List<Ontology> findAllByActiveTrue();

    List<Ontology> findAllByActiveTrueAndAvailableForGeneSearchTrue();

    List<Ontology> findAllByNameIn( List<String> asList );

    Optional<Ontology> findByName( String name );

    Optional<Ontology> findByNameAndActiveTrue( String name );

    boolean existsByName( String name );

    /**
     * Count active ontologies.
     */
    long countByActiveTrue();

    @Query("select count(distinct o) from Ontology o where o.active is true and size(o.terms) <= :maxSize")
    long countByActiveAndTermsSizeLessThanEqual( int maxSize );

    @Query("select count(distinct o) from Ontology o where o.active is true and size(o.terms) > :minSizeExclusive")
    long countByActiveAndTermsSizeGreaterThan( int minSizeExclusive );

    /**
     * Count the number of used terms in an ontology.
     * <p>
     * Only terms from enabled users are counted.
     */
    @Query("select count(distinct ut.termInfo) from UserOntologyTerm ut where ut.user.enabled = true and ut.ontology = :ontology")
    long countDistinctUserTermsByOntology( @Param("ontology") Ontology ontology );

    @Modifying
    @Query("update Ontology o set o.active = true where o = :ontology")
    void activate( @Param("ontology") Ontology ontology );

    @Modifying
    @Query("update Ontology o set o.active = false where o = :ontology")
    void deactivate( @Param("ontology") Ontology ontology );
}
