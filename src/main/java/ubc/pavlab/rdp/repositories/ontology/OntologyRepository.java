package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.ontology.Ontology;

import java.util.List;

/**
 * @author poirigui
 */
@Repository
public interface OntologyRepository extends JpaRepository<Ontology, Integer> {

    List<Ontology> findAllByActiveTrue();

    List<Ontology> findAllByNameIn( List<String> asList );

    boolean existsByName( String name );
}
