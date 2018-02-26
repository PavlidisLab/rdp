package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.Set;

@Repository
public interface UserGeneRepository extends JpaRepository<UserGene, Integer> {
    Integer countByTierIn( Collection<TierType> tiers );
    Integer countDistinctGeneByTierIn( Collection<TierType> tiers );
    Integer countDistinctUserByTaxon( Taxon taxon );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneId(int geneId);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTier(int geneId, TierType tier);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findByGeneIdAndTierIn(int geneId, Set<TierType> tiers);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxon(String symbolContaining, Taxon taxon);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTier(String symbolContaining, Taxon taxon, TierType tier);
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<UserGene> findBySymbolContainingIgnoreCaseAndTaxonAndTierIn(String symbolContaining, Taxon taxon, Set<TierType> tiers);

}
