package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.Collection;

@Repository
public interface UserGeneRepository extends JpaRepository<UserGene, Integer> {
    Integer countByTierIn( Collection<TierType> tiers );
    Integer countDistinctGeneByTierIn( Collection<TierType> tiers );
    Integer countDistinctUserByGeneTaxon( Taxon taxon );
}
