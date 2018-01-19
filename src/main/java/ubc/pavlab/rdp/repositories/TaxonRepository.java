package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;

import java.util.List;

@Repository
public interface TaxonRepository extends JpaRepository<Taxon, Integer> {
    Taxon findByCommonName(String commonName);
    List<Taxon> findByActivatedTrue();
}
