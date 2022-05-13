package ubc.pavlab.rdp.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
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

    /**
     * Anonymize the given user.
     * <p>
     * The {@link User#getId()} is replaced by zero, an {@link User#getAnonymousId()} is generated and the following
     * fields are exposed in the returned object: researcher categories and organ systems.
     * <p>
     * The original user is stored in a cache so that it can be retrieved by {@link #findUserByAnonymousIdNoAuth(UUID)}
     * using the anonymized ID.
     * <p>
     * Note: when using this, ensure that the user is enabled as per {@link User#isEnabled()}, otherwise a
     * {@link org.springframework.security.access.AccessDeniedException} will be raised. That is because a non-enabled
     * user will not satisfy the 'read' permission for any user as defined in {@link ubc.pavlab.rdp.security.PermissionEvaluatorImpl}
     * and {@link PrivacyService#checkUserCanSee(User, UserContent)}.
     *
     * @throws org.springframework.security.access.AccessDeniedException if the user is not enabled
     */
    User anonymizeUser( User user );

    /**
     * Anonymize the given gene.
     * <p>
     * The {@link UserGene#getUser()} is anonymized in the process as per {@link #anonymizeUser(User)}. the {@link UserGene#getId()}
     * is set to zero, an {@link UserGene#getAnonymousId()} is generated. The following fields are preserved in the
     * returned object: gene ID, symbol, name taxon, modification date, tier.
     * <p>
     * The original gene is stored in a cache so that it can be retrieved by {@link #findUserGeneByAnonymousIdNoAuth(UUID)}
     * using the anonymized ID.
     * <p></p>
     * Note: when using this, ensure that the user associated to the gene is enabled as per {@link User#isEnabled()},
     * otherwise a {@link org.springframework.security.access.AccessDeniedException} will be raised.
     *
     * @throws org.springframework.security.access.AccessDeniedException if the corresponding user of the gene is not
     *                                                                   enabled
     */
    UserGene anonymizeUserGene( UserGene userGene );

    void revokeAccessToken( AccessToken accessToken );

    AccessToken createAccessTokenForUser( User user );

    Optional<User> getRemoteSearchUser();

    Collection<User> findAll();

    Page<User> findByEnabledTrueNoAuth( Pageable pageable );

    Page<User> findByEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType privacyLevel, Pageable pageable );

    /**
     * Find users by their name.
     * <p>
     * Note: results are sorted according to {@link #getUserComparator()}.
     */
    List<User> findByLikeName( String nameLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<OrganInfo> userOrgans );

    /**
     * Find users by their name using a prefix match.
     * <p>
     * Note: results are sorted according to {@link #getUserComparator()}.
     */
    List<User> findByStartsName( String startsName, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<OrganInfo> userOrgans );

    /**
     * Find users by their description and sorted according to {@link #getUserComparator()}.
     * <p>
     * Note: results are sorted according to {@link #getUserComparator()}.
     */
    List<User> findByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<OrganInfo> userOrgans );

    long countResearchers();

    long countPublicResearchers();

    UserTerm convertTerm( User user, Taxon taxon, GeneOntologyTermInfo term );

    Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTermInfo> terms );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon );

    Collection<UserTerm> recommendTerms( User user, Taxon taxon, long minSize, long maxSize, long minFrequency );

    User updateTermsAndGenesInTaxon( User user,
                                     Taxon taxon,
                                     Map<GeneInfo, TierType> genesToTierMapFrom,
                                     Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap,
                                     Collection<GeneOntologyTermInfo> goTerms );

    User updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( User user, Profile profile, Set<Publication> publications, Set<String> organUberonIds, Map<String, List<String>> termIdsByOntologyId, Locale locale );

    PasswordResetToken createPasswordResetTokenForUser( User user, Locale locale );

    PasswordResetToken verifyPasswordResetToken( int userId, String token ) throws TokenException;

    User changePasswordByResetToken( int userId, String token, PasswordReset passwordReset ) throws TokenException;

    VerificationToken createVerificationTokenForUser( User user, Locale locale );

    VerificationToken createContactEmailVerificationTokenForUser( User user, Locale locale );

    User confirmVerificationToken( String token ) throws TokenException;

    SortedSet<String> getLastNamesFirstChar();

    void updateUserTerms();

    long computeTermOverlaps( UserTerm userTerm, Collection<GeneInfo> genes );

    long computeTermFrequencyInTaxon( User user, GeneOntologyTerm term, Taxon taxon );

    void sendGeneAccessRequest( User requestingUser, UserGene userGene, String reason );
}
