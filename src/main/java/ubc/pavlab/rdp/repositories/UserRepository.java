package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    User findByEmail(String email);
    Collection<User> findByProfileNameContainingOrProfileLastNameContaining( String nameLike, String lastNameLike );

    @Query("select distinct u from User u inner join u.taxonDescriptions td where u.profile.description like %:descriptionLike% or td like %:descriptionLike%")
    Collection<User> findByDescription( @Param("descriptionLike") String descriptionLike );

    @Query("select count(distinct user_id) FROM UserGene")
    Integer countWithGenes();

    @Query("select distinct u from User u inner join u.userGenes ug where ug = :gene")
    Collection<User> findByGene( @Param("gene") Gene gene);

    @Query("select distinct u from User u inner join u.userGenes ug where ug = :gene and ug.tier = :tier")
    Collection<User> findByGene( @Param("gene") Gene gene, @Param("tier") TierType tier );

    @Query("select distinct u from User u inner join u.userGenes ug where ug.symbol like %:symbol% and ug.taxon = :taxon")
    Collection<User> findByGeneSymbolLike( @Param("symbol") String symbol, @Param("taxon") Taxon taxon );

    @Query("select distinct u from User u inner join u.userGenes ug where ug.symbol like %:symbol% and ug.taxon = :taxon and ug.tier = :tier")
    Collection<User> findByGeneSymbolLike( @Param("symbol") String symbol, @Param("taxon") Taxon taxon, @Param("tier") TierType tier );

}
