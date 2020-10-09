package ubc.pavlab.rdp.services;

import org.springframework.security.authentication.BadCredentialsException;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.validation.ValidationException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    User create( User user );

    User createAdmin( User admin );

    User update( User user );

    void delete( User user );

    User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    User findUserById( int id );

    User findUserByIdNoAuth( int id );

    User findUserByEmailNoAuth( String email );

    User getRemoteAdmin();

    Collection<User> findAll();

    Collection<User> findByLikeName( String nameLike, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<User> findByStartsName( String startsName, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<User> findByDescription( String descriptionLike, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    long countResearchers();

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon, long minSize, long maxSize, long minFrequency );

    User updateTermsAndGenesInTaxon( User user,
                                     Taxon taxon,
                                     Map<GeneInfo, TierType> genesToTierMapFrom,
                                     Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap,
                                     Collection<GeneOntologyTerm> goTerms );

    User updateUserProfileAndPublicationsAndOrgans( User user, Profile profile, Set<Publication> publications, Set<String> organUberonIds );

    PasswordResetToken createPasswordResetTokenForUser( User user, String token );

    void verifyPasswordResetToken( int userId, String token ) throws TokenException;

    User changePasswordByResetToken( int userId, String token, PasswordReset passwordReset ) throws TokenException;

    VerificationToken createVerificationTokenForUser( User user, String token );

    User confirmVerificationToken( String token ) throws TokenException;

    SortedSet<String> getLastNamesFirstChar();

    void updateUserTerms();

    long computeTermOverlaps( UserTerm userTerm, Collection<GeneInfo> genes );

    long computeTermFrequency( UserTerm userTerm );
}
