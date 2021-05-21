package ubc.pavlab.rdp.services;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.validation.ValidationException;
import java.util.*;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    User create( User user );

    User createAdmin( User admin );

    User createServiceAccount( User user );

    User update( User user );

    void delete( User user );

    User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException;

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    User findUserById( int id );

    User findUserByAnonymousIdNoAuth( UUID anonymousId );

    UserGene findUserGeneByAnonymousIdNoAuth( UUID anonymousId );

    User findUserByIdNoAuth( int id );

    User findUserByEmailNoAuth( String email );

    User findUserByAccessTokenNoAuth( String accessToken ) throws TokenException;

    User anonymizeUser( User user );

    UserGene anonymizeUserGene( UserGene userGene );

    void revokeAccessToken( AccessToken accessToken );

    AccessToken createAccessTokenForUser( User user );

    User getRemoteAdmin();

    Collection<User> findAll();

    Collection<User> findByLikeName( String nameLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<User> findByStartsName( String startsName, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<User> findByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    long countResearchers();

    UserTerm convertTerm( User user, Taxon taxon, GeneOntologyTermInfo term );

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTermInfo> terms );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon, long minSize, long maxSize, long minFrequency );

    User updateTermsAndGenesInTaxon( User user,
                                     Taxon taxon,
                                     Map<GeneInfo, TierType> genesToTierMapFrom,
                                     Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap,
                                     Collection<GeneOntologyTermInfo> goTerms );

    User updateUserProfileAndPublicationsAndOrgans( User user, Profile profile, Set<Publication> publications, Set<String> organUberonIds );

    PasswordResetToken createPasswordResetTokenForUser( User user );

    PasswordResetToken verifyPasswordResetToken( int userId, String token ) throws TokenException;

    User changePasswordByResetToken( int userId, String token, PasswordReset passwordReset ) throws TokenException;

    VerificationToken createVerificationTokenForUser( User user );

    @Transactional
    VerificationToken createContactEmailVerificationTokenForUser( User user );

    User confirmVerificationToken( String token ) throws TokenException;

    SortedSet<String> getLastNamesFirstChar();

    void updateUserTerms();

    long computeTermOverlaps( UserTerm userTerm, Collection<GeneInfo> genes );

    long computeTermFrequencyInTaxon( User user, GeneOntologyTerm term, Taxon taxon );
}
