package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;

import java.util.List;

/**
 * @author poirigui
 */
@Repository
public interface OntologyRepository extends JpaRepository<Ontology, Integer> {

    List<Ontology> findAllByActiveTrue();

    List<Ontology> findAllByActiveTrueAndAvailableForGeneSearchTrue();
    List<Ontology> findAllByNameIn( List<String> asList );

    Ontology findByName( String name );

    boolean existsByName( String name );

    /**
     * Count active ontologies.
     */
    long countByActiveTrue();

    @Modifying
    @Query("update Ontology o set o.active = true where o = :ontology")
    void activate( @Param("ontology") Ontology ontology );

    @Modifying
    @Query("update Ontology o set o.active = false where o = :ontology")
    void deactivate( @Param("ontology") Ontology ontology );
}
