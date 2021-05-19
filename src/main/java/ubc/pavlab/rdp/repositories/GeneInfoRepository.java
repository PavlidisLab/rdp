package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

@Repository
public interface GeneInfoRepository extends JpaRepository<GeneInfo, Integer> {

    GeneInfo findByGeneId( Integer geneId );

    @Query("select gene from GeneInfo gene left join fetch gene.orthologs where gene.geneId = :geneId")
    GeneInfo findByGeneIdWithOrthologs( @Param("geneId") Integer geneId );

    Collection<GeneInfo> findAllByGeneIdIn( Collection<Integer> geneIds );

    GeneInfo findByGeneIdAndTaxon( Integer geneId, Taxon taxon );

    GeneInfo findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    Collection<GeneInfo> findAllBySymbolAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllBySymbolStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByNameStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByAliasesContainingIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByTaxonActiveTrue();

    long countByTaxon( Taxon taxon );
}
