package ubc.pavlab.rdp.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.ontology.Ontology;

import javax.persistence.QueryHint;
import java.util.Collection;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    @Query("select user from User user left join fetch user.roles where user.id = :id")
    Optional<User> findOneWithRoles( @Param("id") int id );

    @Query("select user from User user left join fetch user.userTerms")
    Collection<User> findAllWithUserTerms();

    boolean existsByUserOntologyTermsOntology( Ontology ontology );

    long countByEnabledTrue();

    long countByEnabledTrueAndProfilePrivacyLevel( PrivacyLevelType aPublic );

    /**
     * Find all enabled users.
     */
    Page<User> findByEnabledTrue( Pageable pageable );

    Page<User> findByEnabledTrueAndProfilePrivacyLevel( PrivacyLevelType privacyLevel, Pageable pageable );

    @Query("select user from User user left join fetch user.roles where lower(user.email) = lower(:email)")
    User findByEmailIgnoreCase( @Param("email") String email );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select u from User u where u.profile.lastName is not NULL and u.profile.lastName <> '' and u.profile.privacyLevel >= :privacyLevel")
    Collection<User> findAllWithNonEmptyProfileLastNameAndProfilePrivacyLevelGreaterOrEqualThan( @Param("privacyLevel") PrivacyLevelType privacyLevel );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileLastNameStartsWithIgnoreCase( String lastNameLike );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Collection<User> findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( String nameLike,
                                                                                                 String lastNameLike );

    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select distinct u from User u left join u.taxonDescriptions t left join u.userOrgans uog left join u.userOntologyTerms uot where upper(u.profile.description) like upper(:descriptionLike) or upper(t) like upper(:descriptionLike) or upper (uog.name) like upper(:descriptionLike) or upper(uot.name) like upper(:descriptionLike)")
    Collection<User> findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( String descriptionLike );

    /**
     * Obtain users whose last name and research interests matches the given pattern.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select distinct u from User u left join u.taxonDescriptions t left join u.userOrgans uog left join u.userOntologyTerms uot where upper(u.profile.lastName) like upper(:lastNameLike) and (upper(u.profile.description) like upper(:descriptionLike) or upper(t) like upper(:descriptionLike) or upper (uog.name) like upper(:descriptionLike) or upper(uot.name) like upper(:descriptionLike))")
    List<User> findDistinctByProfileLastNameLikeIgnoreCaseAndProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCase(
            @Param("lastNameLike") String lastNameLike, @Param("descriptionLike") String descriptionLike );

    /**
     * Obtain users whose full name and research interests matches the given patterns.
     * <p>
     * The research interests include profile description, taxon descriptions and associated user ontology term
     * descriptions.
     * <p>
     * Note: this query had to be written manually so that the OR takes precedence on the AND. Otherwise, a much longer
     * and convoluted name would be necessary.
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    @Query("select distinct u from User u left join u.taxonDescriptions t left join u.userOrgans uog left join u.userOntologyTerms uot where (upper(u.profile.lastName) like upper(:nameLike) or upper(u.profile.name) like upper(:nameLike)) and (upper(u.profile.description) like upper(:descriptionLike) or upper(t) like upper(:descriptionLike) or upper (uog.name) like upper(:descriptionLike) or upper(uot.name) like upper(:descriptionLike))")
    List<User> findDistinctByProfileFullNameLikeIgnoreCaseAndProfileDescriptionLikeIgnoreCaseAndTaxonDescriptionsLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCase(
            @Param("nameLike") String nameLike, @Param("descriptionLike") String descriptionLike );
}
