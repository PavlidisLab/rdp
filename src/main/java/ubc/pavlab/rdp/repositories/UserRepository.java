package ubc.pavlab.rdp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.Organ;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    @Query("select user from User user left join fetch user.roles where user.id = :id")
    User findOneWithRoles( @Param("id") int id );

    @Query("select user from User user left join fetch user.userTerms")
    Collection<User> findAllWithUserTerms();

    @Override
    long count();

    long countByProfilePrivacyLevel( PrivacyLevelType aPublic );

    /**
     * Find all enabled users.
     */
    Page<User> findByEnabledTrue( Pageable pageable );

    Page<User> findByEnabledTrueAndProfilePrivacyLevel( PrivacyLevelType privacyLevel, Pageable pageable );

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

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findDistinctByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase(
            String descriptionLike, String taxonDescriptionLike );
}
