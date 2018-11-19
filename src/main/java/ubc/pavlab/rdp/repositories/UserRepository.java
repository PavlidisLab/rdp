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
    @Cacheable(cacheNames="stats", key = "#root.methodName")
    long count();

    User findByEmailIgnoreCase(String email);

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( String nameLike, String lastNameLike );

    @SuppressWarnings("SpringDataRepositoryMethodParametersInspection")
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( String descriptionLike, String taxonDescriptionLike );



}
