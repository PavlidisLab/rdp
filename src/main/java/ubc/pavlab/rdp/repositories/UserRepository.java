package ubc.pavlab.rdp.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.User;

import javax.persistence.QueryHint;
import java.util.Collection;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select user from User user left join fetch user.roles where user.id = :id")
    User findOneWithRoles( @Param("id") int id );

    @Query("select user from User user left join fetch user.userTerms")
    Collection<User> findAllWithUserTerms();

    @SuppressWarnings("SpringCacheAnnotationsOnInterfaceInspection")
    @Override
    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    long count();

    @Query("select user from User user left join fetch user.roles where lower(user.email) = lower(:email)")
    User findByEmailIgnoreCase( @Param("email") String email );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select u from User u where u.profile.lastName is not NULL and u.profile.lastName <> ''")
    Collection<User> findAllWithNonEmptyProfileLastName();

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileLastNameStartsWithIgnoreCase( String lastNameLike );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( String nameLike,
            String lastNameLike );

    @SuppressWarnings("SpringDataRepositoryMethodParametersInspection")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase(
            String descriptionLike, String taxonDescriptionLike );
}
