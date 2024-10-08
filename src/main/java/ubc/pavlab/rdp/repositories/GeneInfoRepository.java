package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface GeneInfoRepository extends JpaRepository<GeneInfo, Integer> {

    Optional<GeneInfo> findByGeneId( Integer geneId );

    Collection<GeneInfo> findAllByGeneIdIn( Collection<Integer> geneIds );

    @Query("select gene from GeneInfo gene left join fetch gene.orthologs where gene.geneId in :geneIds")
    Collection<GeneInfo> findAllByGeneIdWithOrthologs( @Param("geneIds") Collection<Integer> geneIds );

    Optional<GeneInfo> findByGeneIdAndTaxon( Integer geneId, Taxon taxon );

    Optional<GeneInfo> findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    Collection<GeneInfo> findAllBySymbolAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllBySymbolStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByNameStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByAliasesContainingIgnoreCaseAndTaxon( String query, Taxon taxon );

    long countByTaxon( Taxon taxon );
}
