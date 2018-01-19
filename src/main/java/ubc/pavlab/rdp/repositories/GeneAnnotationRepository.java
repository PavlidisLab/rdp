package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneAnnotation;
import ubc.pavlab.rdp.model.GeneAnnotationId;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.AggregateCount;

import java.util.Collection;
import java.util.List;

@Repository
public interface GeneAnnotationRepository extends JpaRepository<GeneAnnotation, GeneAnnotationId> {

    Collection<GeneAnnotation> findByGoId( String go);
    Collection<GeneAnnotation> findByGoIdAndGeneTaxon( String go, Taxon taxon);
    Collection<GeneAnnotation> findByGene(Gene gene);
    GeneAnnotation findByGoIdAndGene(String go, Gene gene);
    Integer countByGoId(String go);
    Integer countByGoIdAndGeneTaxon(String go, Taxon taxon);

    @Query("SELECT new ubc.pavlab.rdp.util.AggregateCount( goId, taxon, COUNT(*) as count) from GeneAnnotation ga group by ga.goId, ga.taxon")
    List<AggregateCount> calculateDirectSizes();


}
