package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;
import java.util.List;

@Repository
public interface GeneRepository extends JpaRepository<Gene, Integer> {
    Collection<Gene> findBySymbol( String officialSymbol );

    Gene findBySymbolAndTaxon( String officialSymbol, Taxon taxon );

    Collection<Gene> findByTaxonId( Integer id );

    Collection<Gene> findByIdIn( Collection<Integer> ids );

    /* Searching */
    Collection<Gene> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    @Query("select g from Gene g where g.taxon = :taxon and (g.symbol = :symbol or g.symbol like %:symbolLike or g.name like %:nameLike% or g.aliases like %:aliasesLike%)")
    List<Gene> autocomplete( @Param("symbol") String symbol, @Param("symbolLike") String symbolLike, @Param("nameLike") String nameLike, @Param("aliasesLike") String aliasesLike, @Param("taxon") Taxon taxon );
    //Collection<Gene> findBySymbolOrSymbolStartingWithOrNameContainingOrAliasesContaining(String query)
}
