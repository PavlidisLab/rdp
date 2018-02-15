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
    User create( User user );

    @Transactional
    User update( User user );

    @Secured( "ROLE_ADMIN" )
    @Transactional
    void delete( User user );

    @Transactional
    User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

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

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByGene( Gene gene );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByGene( Gene gene, TierType tier );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByGene( Gene gene, Set<TierType> tiers );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByLikeSymbol( String symbol, Taxon taxon );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByLikeSymbol( String symbol, Taxon taxon, TierType tier );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByLikeName( String nameLike );

    @Secured( {"ROLE_ADMIN", "ROLE_MANAGER"} )
    Collection<User> findByDescription( String descriptionLike );

    long countResearchers();

    Integer countResearchersWithGenes();

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms );

    UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

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
    User changePasswordByResetToken( int userId, String token, String newPassword );

    @Transactional
    void createVerificationTokenForUser( User user, String token );

    @Transactional
    User confirmVerificationToken( String token );
}
