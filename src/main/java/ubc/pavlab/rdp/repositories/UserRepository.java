package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene.TierType;

import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByEmail(String email);
    Collection<User> findByProfileNameContainingOrProfileLastNameContaining( String nameLike, String lastNameLike );

    @Query("select count(distinct user_id) FROM UserGene")
    Integer countWithGenes();

    @Query("select u from User u inner join u.geneAssociations ga where ga.pk.gene = :gene")
    Collection<User> findByGene( @Param("gene") Gene gene);

    @Query("select u from User u inner join u.geneAssociations ga where ga.pk.gene = :gene and ga.tier = :tier")
    Collection<User> findByGene( @Param("gene") Gene gene, @Param("tier") TierType tier );

    @Query("select u from User u inner join u.geneAssociations ga where ga.pk.gene.symbol like %:symbol% and ga.pk.gene.taxon = :taxon")
    Collection<User> findByGeneSymbolLike( @Param("symbol") String symbol, @Param("taxon") Taxon taxon );

    @Query("select u from User u inner join u.geneAssociations ga where ga.pk.gene.symbol like %:symbol% and ga.pk.gene.taxon = :taxon and ga.tier = :tier")
    Collection<User> findByGeneSymbolLike( @Param("symbol") String symbol, @Param("taxon") Taxon taxon, @Param("tier") TierType tier );

}
