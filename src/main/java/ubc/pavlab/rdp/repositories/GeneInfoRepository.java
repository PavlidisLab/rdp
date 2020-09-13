package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.util.MatchType;

import java.util.Collection;

@Repository
public interface GeneInfoRepository extends JpaRepository<GeneInfo, Integer> {

    GeneInfo findByGeneId( Integer geneId );

    Collection<GeneInfo> findAllByIdIn( Collection<Integer> ids );

    Collection<GeneInfo> findAllByGeneIdIn( Collection<Integer> geneIds );

    GeneInfo findByGeneIdAndTaxon ( Integer geneId, Taxon taxon );

    GeneInfo findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    Collection<GeneInfo> findAllBySymbol( String symbol );

    Collection<GeneInfo> findAllBySymbolStartingWithIgnoreCase( String symbolPrefix );

    Collection<GeneInfo> findAllByNameStartingWithIgnoreCase( String query );

    Collection<GeneInfo> findAllByAliasesContainingIgnoreCase( String query );

    Collection<GeneInfo> findAllBySymbolAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllBySymbolStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByNameStartingWithIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByAliasesContainingIgnoreCaseAndTaxon( String query, Taxon taxon );

    Collection<GeneInfo> findAllByTaxonActiveTrue();
}
