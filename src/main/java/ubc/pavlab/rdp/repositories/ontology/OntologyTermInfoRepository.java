package ubc.pavlab.rdp.repositories.ontology;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.util.Arrays;
import java.util.List;

/**
 * @author poirigui
 */
@Repository
public interface OntologyTermInfoRepository extends JpaRepository<OntologyTermInfo, Integer> {

    /**
     * One or more ontologies may share the same term identifier.
     */
    List<OntologyTermInfo> findByTermId( String termId );

    OntologyTermInfo findByTermIdAndOntologyId( String termId, Integer ontologyId );

    List<OntologyTermInfo> findAllByActiveTrueAndOntologyActiveTrue();
}
