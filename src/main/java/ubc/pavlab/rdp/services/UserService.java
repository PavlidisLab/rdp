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
import java.util.Map;
import java.util.Set;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    /**
     * Privacy level designating that only the administrator can access the users data.
     */
    Integer PRIVACY_PRIVATE = 0;
    /**
     * Privacy level designating that only the locally registered users can access the users data.
     */
    Integer PRIVACY_REGISTERED = 1;
    /**
     * Privacy level designating that everybody can access the users data.
     */
    Integer PRIVACY_PUBLIC = 2;

    @Transactional
    User create( User user );

    @Transactional
    User update( User user );

    @Secured("ROLE_ADMIN")
    @Transactional
    void delete( User user );

    @Transactional
    User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    User findUserById( int id );

    User findUserByIdNoAuth( int id );

    User findUserByEmail( String email );

    User getRemoteAdmin();

    List<User> findAll();

    Collection<User> findByLikeName( String nameLike );

    Collection<User> findByStartsName( String startsName );

    Collection<User> findByDescription( String descriptionLike );

    long countResearchers();

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms );

    UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon, int minSize, int maxSize, int minFrequency );

    @Transactional
    void updateTermsAndGenesInTaxon( User user, Taxon taxon, Map<Gene, TierType> genesToTierMap,
            Collection<GeneOntologyTerm> goTerms );

    @Transactional
    User updatePublications( User user, Set<Publication> publications );

    @Transactional
    PasswordResetToken createPasswordResetTokenForUser( User user, String token );

    void verifyPasswordResetToken( int userId, String token ) throws TokenException;

    @Transactional
    User changePasswordByResetToken( int userId, String token, String newPassword );

    @Transactional
    VerificationToken createVerificationTokenForUser( User user, String token );

    @Transactional
    User confirmVerificationToken( String token );

    boolean checkCurrentUserCanSee( User user );

    boolean checkCurrentUserCanSee( UserGene userGene );

    List<String> getChars();
}
