package ubc.pavlab.rdp.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;

import java.util.List;

@Repository
public interface TaxonRepository extends JpaRepository<Taxon, Integer> {

    @Cacheable("taxon-list")
    List<Taxon> findByActiveTrue();
}
