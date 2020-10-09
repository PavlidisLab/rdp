package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;

import javax.persistence.QueryHint;
import java.util.List;

@Repository
public interface TaxonRepository extends JpaRepository<Taxon, Integer> {

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    List<Taxon> findByActiveTrueOrderByOrdering();
}
