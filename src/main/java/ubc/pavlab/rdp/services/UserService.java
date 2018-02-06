package ubc.pavlab.rdp.services;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.validation.ValidationException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    @Transactional
    void create( User user );

    @Transactional
    void update( User user );

    @Secured( "ROLE_ADMIN" )
    @Transactional
    void delete( User user );

    @Transactional
    void changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    User findUserById( int id );

    User findUserByIdNoAuth( int id );

    User findUserByEmail( String email );

    User findUserByUserName( String email );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    List<User> findAll();

    Collection<User> findByGene( Gene gene );

    Collection<User> findByGene( Gene gene, TierType tier );

    Collection<User> findByGene( Gene gene, Set<TierType> tiers );

    Collection<User> findByLikeSymbol( String symbol, Taxon taxon );

    Collection<User> findByLikeSymbol( String symbol, Taxon taxon, TierType tier );

    Collection<User> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByLikeName( String nameLike );

    Collection<User> findByDescription( String descriptionLike );

    long countResearchers();

    Integer countResearchersWithGenes();

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms );

    UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    @Transactional
    void addGenesToUser( User user, Collection<UserGene> genes );

    @Transactional
    void removeGenesFromUser( User user, Collection<Gene> genes );

    @Transactional
    void updateGenesInTaxon( User user, Taxon taxon, Collection<UserGene> genes );

    @Transactional
    void updateGOTermsInTaxon( User user, Taxon taxon, Collection<GeneOntologyTerm> goTerms);

    @Transactional
    void updateTermsAndGenesInTaxon( User user, Taxon taxon, Collection<UserGene> genes, Collection<GeneOntologyTerm> goTerms );

    @Transactional
    void updatePublications( User user, Set<Publication> publications );

    @Transactional
    void createPasswordResetTokenForUser( User user, String token );

    void verifyPasswordResetToken( int userId, String token ) throws TokenException;

    @Transactional
    void changePasswordByResetToken( int userId, String token, String newPassword );

    @Transactional
    void createVerificationTokenForUser( User user, String token );

    @Transactional
    User confirmVerificationToken( String token );
}
