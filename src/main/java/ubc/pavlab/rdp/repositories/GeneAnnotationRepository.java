package ubc.pavlab.rdp.repositories;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneAnnotation;
import ubc.pavlab.rdp.model.GeneAnnotationId;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;
import java.util.List;

@Repository
public interface GeneAnnotationRepository extends JpaRepository<GeneAnnotation, GeneAnnotationId> {

    @Getter
    @AllArgsConstructor
    class AggregateCount {
        private String geneOntologyId;
        private Taxon taxon;
        private Integer count;
    }

    Collection<GeneAnnotation> findByPkGoId( String go);
    Collection<GeneAnnotation> findByPkGoIdAndPkGeneTaxon( String go, Taxon taxon);
    Collection<GeneAnnotation> findByPkGene(Gene gene);
    GeneAnnotation findByPkGoIdAndPkGene(String go, Gene gene);
    Integer countByPkGoId(String go);
    Integer countByPkGoIdAndPkGeneTaxon(String go, Taxon taxon);

    @Query("SELECT new ubc.pavlab.rdp.repositories.GeneAnnotationRepository.AggregateCount( geneOntologyId, taxon, COUNT(*) as count) from gene_annotation group by geneOntologyId, taxon")
    List<AggregateCount> calculateDirectSizes();


}
