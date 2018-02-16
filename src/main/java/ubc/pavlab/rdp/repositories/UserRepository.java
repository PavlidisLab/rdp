package ubc.pavlab.rdp.repositories;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Cacheable(cacheNames="stats", key = "#root.methodName")
    long count();

    User findByEmail(String email);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameContainingOrProfileLastNameContaining( String nameLike, String lastNameLike );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select distinct u from User u inner join u.taxonDescriptions td where u.profile.description like %:descriptionLike% or td like %:descriptionLike%")
    Collection<User> findByDescription( @Param("descriptionLike") String descriptionLike );

    @Cacheable(cacheNames="stats", key = "#root.methodName")
    @Query("select count(distinct user_id) FROM UserGene")
    Integer countWithGenes();

}
