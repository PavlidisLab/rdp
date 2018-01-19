package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;

import java.util.Collection;

@Repository
public interface UserGeneRepository extends JpaRepository<UserGene, Integer> {
    Integer countByTierIn( Collection<UserGene.TierType> tiers );
    Integer countDistinctGeneByTierIn( Collection<UserGene.TierType> tiers );
    Integer countDistinctUserByPkGeneTaxon( Taxon taxon );
}
