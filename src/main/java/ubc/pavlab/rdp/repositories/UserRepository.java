package ubc.pavlab.rdp.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.User;

import javax.persistence.QueryHint;
import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @SuppressWarnings("SpringCacheAnnotationsOnInterfaceInspection")
    @Override
    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    long count();

    User findByEmailIgnoreCase( String email );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameStartsWithIgnoreCaseOrProfileLastNameStartsWithIgnoreCase( String nameLike,
            String lastNameLike );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( String nameLike,
            String lastNameLike );

    @SuppressWarnings("SpringDataRepositoryMethodParametersInspection")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase(
            String descriptionLike, String taxonDescriptionLike );

    //    @Query("select distinct substring(u.profile.name, 1, 1) FROM User u where u.profile.name is not null and length(u.profile.name) > 0")
    //    Set<String> getNameChars();
    //
    //    @Query("select distinct substring(u.profile.lastName, 1, 1) FROM User u where u.profile.lastName is not null and length(u.profile.lastName) > 0")
    //    Set<String> getLastNmeChars();
}
