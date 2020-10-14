package ubc.pavlab.rdp.services;

import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.validation.ValidationException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static org.springframework.util.CollectionUtils.containsAny;

/**
 * Created by mjacobson on 16/01/18.
 */
@Service("userService")
@CommonsLog
public class UserServiceImpl implements UserService {

    @Autowired
    ApplicationSettings applicationSettings;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired
    private VerificationTokenRepository tokenRepository;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private GOService goService;
    @Autowired
    private OrganInfoService organInfoService;
    @Autowired
    UserGeneRepository userGeneRepository;

    @Transactional
    @Override
    public User create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( "ROLE_USER" );
        user.setRoles( Collections.singleton( userRole ) );
        return userRepository.save( user );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    @Override
    public User createAdmin( User admin ) {
        admin.setPassword( bCryptPasswordEncoder.encode( admin.getPassword() ) );
        Role adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        admin.setRoles( Collections.singleton( adminRole ) );
        return userRepository.save( admin );
    }

    @Override
    @PreAuthorize("hasPermission(#user, 'update')")
    public User update( User user ) {
        if ( applicationSettings.getPrivacy() == null ) {
            // FIXME: this should not be possible...
            log.warn( MessageFormat.format( "{0} attempted to update, but applicationSettings.privacy is null.", user.getEmail() ) );
        } else {
            PrivacyLevelType defaultPrivacyLevel = PrivacyLevelType.values()[applicationSettings.getPrivacy().getDefaultLevel()];
            boolean defaultSharing = applicationSettings.getPrivacy().isDefaultSharing();
            boolean defaultGenelist = applicationSettings.getPrivacy().isAllowHideGenelist();

            if ( user.getProfile().getPrivacyLevel() == null ) {
                log.warn( "Received a null 'privacyLevel' value in profile." );
                user.getProfile().setPrivacyLevel( defaultPrivacyLevel );
            }

            if ( user.getProfile().getShared() == null ) {
                log.warn( "Received a null 'shared' value in profile." );
                user.getProfile().setShared( defaultSharing );
            }

            if ( user.getProfile().getHideGenelist() == null ) {
                if ( applicationSettings.getPrivacy().isAllowHideGenelist() ) {
                    log.warn( "Received a null 'hideGeneList' value in profile." );
                }
                user.getProfile().setHideGenelist( defaultGenelist );
            }
        }

        PrivacyLevelType userPrivacyLevel = user.getProfile().getPrivacyLevel();

        // We cap the user gene privacy level to its new profile setting
        for ( UserGene gene : user.getUserGenes().values() ) {
            PrivacyLevelType genePrivacyLevel = gene.getPrivacyLevel();
            // in case any of the user or gene privacy level is null, we already have a cascading value
            if ( userPrivacyLevel == null || genePrivacyLevel == null ) {
                continue;
            }
            if ( userPrivacyLevel.ordinal() < genePrivacyLevel.ordinal() ) {
                gene.setPrivacyLevel( userPrivacyLevel );
                log.info( MessageFormat.format( "Privacy level of {0} will be capped to {1} (was {2}",
                        gene, userPrivacyLevel, genePrivacyLevel ) );
            }
        }

        return userRepository.save( user );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    @Override
    public void delete( User user ) {

        VerificationToken verificationToken = tokenRepository.findByUser( user );

        if ( verificationToken != null ) {
            tokenRepository.delete( verificationToken );
        }

        PasswordResetToken passwordToken = passwordResetTokenRepository.findByUser( user );

        if ( passwordToken != null ) {
            passwordResetTokenRepository.delete( passwordToken );
        }

        userRepository.delete( user );
    }

    @Transactional
    @Override
    public User changePassword( String oldPassword, String newPassword )
            throws BadCredentialsException, ValidationException {
        User user = findCurrentUser();
        if ( bCryptPasswordEncoder.matches( oldPassword, user.getPassword() ) ) {
            if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

                user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
                return update( user );
            } else {
                throw new ValidationException( "Password must be a minimum of 6 characters" );
            }
        } else {
            throw new BadCredentialsException( "Password incorrect" );
        }
    }

    @Override
    public String getCurrentUserName() {
        return getCurrentEmail();
    }

    @Override
    public String getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    @Override
    public User findCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || auth.getPrincipal().equals( "anonymousUser" ) ) {
            return null;
        }

        return findUserByIdNoAuth( ( (UserPrinciple) auth.getPrincipal() ).getId() );
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public User findUserById( int id ) {
        return userRepository.findOne( id );
    }

    @Override
    public User findUserByIdNoAuth( int id ) {
        // Only use this in placed where no authentication of user is needed
        return userRepository.findOneWithRoles( id );
    }

    @Override
    public User findUserByEmailNoAuth( String email ) {
        return userRepository.findByEmailIgnoreCase( email );
    }

    @Override
    public User getRemoteAdmin() {
        return userRepository.findOneWithRoles( applicationSettings.getIsearch().getUserId() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<User> findByLikeName( String nameLike, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans ) {
        return userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( nameLike, nameLike ).stream()
                .filter( u -> researcherTypes == null || containsAny( researcherTypes, u.getProfile().getResearcherCategories() ) )
                .filter( u -> userOrgans == null || containsAny( userOrgans, u.getUserOrgans().values() ) )
                .collect( Collectors.toSet() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<User> findByStartsName( String startsName, Set<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans ) {
        return userRepository.findByProfileLastNameStartsWithIgnoreCase( startsName ).stream()
                .filter( u -> researcherTypes == null || containsAny( researcherTypes, u.getProfile().getResearcherCategories() ) )
                .filter( u -> userOrgans == null || containsAny( userOrgans, u.getUserOrgans().values() ) )
                .collect( Collectors.toSet() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<User> findByDescription( String descriptionLike, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans ) {
        return userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( descriptionLike, descriptionLike ).stream()
                .filter( u -> researcherTypes == null || containsAny( researcherTypes, u.getProfile().getResearcherCategories() ) )
                .filter( u -> userOrgans == null || containsAny( userOrgans, u.getUserOrgans().values() ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public long countResearchers() {
        return userRepository.count();
    }

    @Override
    public Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms ) {
        return convertTermTypes( user, terms, taxon );
    }

    @Override
    public Collection<UserTerm> recommendTerms( @NonNull User user, @NonNull Taxon taxon ) {
        return recommendTerms( user, taxon, 10, applicationSettings.getGoTermSizeLimit(), 2 );
    }

    @Override
    public Collection<UserTerm> recommendTerms( @NonNull User user, @NonNull Taxon taxon, long minSize, long maxSize, long minFrequency ) {
        Set<GeneInfo> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ).stream()
                .map( UserGene::getGeneInfo )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );

        // Then keep only those terms not already added and with the highest frequency
        Set<UserTerm> topResults = goService.termFrequencyMap( genes ).entrySet().stream()
                .filter( e -> minFrequency < 0 || e.getValue() >= minFrequency )
                .filter( e -> minSize < 0 || e.getKey().getSizeInTaxon( taxon ) >= minSize )
                .filter( e -> maxSize < 0 || e.getKey().getSizeInTaxon( taxon ) <= maxSize )
                .map( e -> UserTerm.createUserTerm( user, e.getKey(), taxon ) )
                .filter( ut -> !user.getUserTerms().contains( ut ) )
                .collect( groupingBy( identity(), maxBy( comparingLong( this::computeTermFrequency ) ) ) )
                .values().stream()
                .map( Optional::get )
                .collect( Collectors.toSet() );

        // Keep only leafiest of remaining terms (keep if it has no descendants in results)
        return topResults.stream()
                .filter( ut -> Collections.disjoint( topResults, convertTerms( user, taxon, goService.getDescendants( ut ) ) ) )
                .collect( Collectors.toSet() );
    }

    @Transactional
    @Override
    @PreAuthorize("hasPermission(#user, 'update')")
    public User updateTermsAndGenesInTaxon( User user,
                                            Taxon taxon,
                                            Map<GeneInfo, TierType> genesToTierMap,
                                            Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap,
                                            Collection<GeneOntologyTerm> goTerms ) {
        Map<Integer, UserGene> userGenes = genesToTierMap.keySet()
                .stream()
                .filter( g -> g.getTaxon().equals( taxon ) )
                .map( g -> {
                    UserGene userGene = user.getUserGenes().getOrDefault( g.getGeneId(), new UserGene() );
                    userGene.setUser( user );
                    userGene.setTier( genesToTierMap.get( g ) );
                    if ( applicationSettings.getPrivacy().isCustomizableGeneLevel() ) {
                        // if no privacy level is set, we inherit the profile value
                        userGene.setPrivacyLevel( genesToPrivacyLevelMap.getOrDefault( g, null ) );
                    }
                    userGene.updateGene( g );
                    return userGene;
                } )
                .collect( Collectors.toMap( Gene::getGeneId, identity() ) );

        // add calculated genes from terms
        Map<Integer, UserGene> userGenesFromTerms = goTerms.stream()
                .flatMap( term -> goService.getGenesInTaxon( term, taxon ).stream() )
                .map( g -> {
                    UserGene userGene = user.getUserGenes().getOrDefault( g.getGeneId(), new UserGene() );
                    userGene.setUser( user );
                    userGene.setTier( genesToTierMap.getOrDefault( g, TierType.TIER3 ) );
                    // we let the privacy level inherit the profile value
                    userGene.updateGene( g );
                    return userGene;
                } )
                .collect( Collectors.toMap( Gene::getGeneId, identity() ) );

        // remove all genes in the taxon
        user.getUserGenes().entrySet()
                .removeIf( e -> e.getValue().getTaxon().equals( taxon ) );

        user.getUserGenes().putAll( userGenes );
        user.getUserGenes().putAll( userGenesFromTerms );

        // update terms
        // go terms with the same identifier will be replaced
        Set<UserTerm> userTerms = convertTermTypes( user, goTerms, taxon );
        user.getUserTerms().removeIf( e -> e.getTaxon().equals( taxon ) && !userTerms.contains( e ) );
        user.getUserTerms().addAll( userTerms );

        return update( user );
    }

    @Override
    public long computeTermOverlaps( UserTerm userTerm, Collection<GeneInfo> genes ) {
        return genes.stream()
                .flatMap( g -> goService.getAllTermsForGene( g, true, true ).stream() )
                .filter( term -> term.getGoId().equals( userTerm.getGoId() ) )
                .count();
    }

    @Override
    public long computeTermFrequency( UserTerm userTerm ) {
        return userTerm.getUser().getUserGenes().values().stream()
                .map( UserGene::getGeneInfo )
                .filter( Objects::nonNull )
                .flatMap( g -> goService.getAllTermsForGene( g, true, true ).stream() )
                .filter( term -> term.getGoId().equals( userTerm.getGoId() ) )
                .count();
    }

    @Transactional
    @Override
    @PreAuthorize("hasPermission(#user, 'update')")
    public User updateUserProfileAndPublicationsAndOrgans( User user, Profile profile, Set<Publication> publications, Set<String> organUberonIds ) {
        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        if ( profile.getResearcherPosition() == null || applicationSettings.getProfile().getEnabledResearcherPositions().contains( profile.getResearcherPosition().name() ) ) {
            user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        } else {
            log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher position to an unknown value {2}.",
                    findCurrentUser(), user, profile.getResearcherPosition() ) );
            if ( user.getProfile().getPrivacyLevel() != profile.getPrivacyLevel() ) {
                user.getUserGenes().values().forEach( ug -> ug.setPrivacyLevel( profile.getPrivacyLevel() ) );
            }
        }
        user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        user.getProfile().setOrganization( profile.getOrganization() );

        if ( profile.getResearcherCategories() != null ) {
            Set<String> researcherCategoryNames = profile.getResearcherCategories().stream()
                    .map( ResearcherCategory::name )
                    .collect( Collectors.toSet() );
            if ( applicationSettings.getProfile().getEnabledResearcherCategories().containsAll( researcherCategoryNames ) ) {
                user.getProfile().setResearcherCategories( profile.getResearcherCategories() );
            } else {
                log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher type to an unknown value {2}.",
                        findCurrentUser(), user, profile.getResearcherCategories() ) );
            }
        }

        user.getProfile().setContactEmail( profile.getContactEmail() );
        user.getProfile().setPhone( profile.getPhone() );
        user.getProfile().setWebsite( profile.getWebsite() );

        // privacy settings
        if ( applicationSettings.getPrivacy().isCustomizableLevel() ) {
            // reset gene privacy levels if the profile value is changed
            if ( applicationSettings.getPrivacy().isCustomizableGeneLevel() &&
                    user.getProfile().getPrivacyLevel() != profile.getPrivacyLevel() ) {
                user.getUserGenes().values().forEach( ug -> ug.setPrivacyLevel( profile.getPrivacyLevel() ) );
            }
            user.getProfile().setPrivacyLevel( profile.getPrivacyLevel() );
        }
        if ( applicationSettings.getPrivacy().isCustomizableSharing() ) {
            user.getProfile().setShared( profile.getShared() );
        }
        if ( applicationSettings.getPrivacy().isAllowHideGenelist() ) {
            user.getProfile().setHideGenelist( profile.getHideGenelist() );
        }

        user.getProfile().getPublications().retainAll( publications );
        user.getProfile().getPublications().addAll( publications );

        if ( applicationSettings.getOrgans().getEnabled() ) {
            Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn( organUberonIds ).stream()
                    .map( organInfo -> user.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo( user, organInfo ) ) )
                    .collect( Collectors.toMap( Organ::getUberonId, identity() ) );
            user.getUserOrgans().clear();
            user.getUserOrgans().putAll( userOrgans );
        }

        return update( user );
    }

    @Transactional
    @Override
    public PasswordResetToken createPasswordResetTokenForUser( User user ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( UUID.randomUUID().toString() );
        return passwordResetTokenRepository.save( userToken );
    }

    @Override
    public PasswordResetToken verifyPasswordResetToken( int userId, String token ) throws TokenException {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken( token );

        if ( passToken == null ) {
            throw new TokenException( "Password reset token is invalid." );
        }

        if ( !passToken.getUser().getId().equals( userId ) ) {
            throw new TokenException( "Password reset token is invalid." );
        }

        if ( Instant.now().isAfter( passToken.getExpiryDate().toInstant() ) ) {
            throw new TokenException( "Password reset token is expired." );
        }

        return passToken;
    }

    @Transactional
    @Override
    public User changePasswordByResetToken( int userId, String token, PasswordReset passwordReset ) throws TokenException, ValidationException {

        PasswordResetToken passToken = verifyPasswordResetToken( userId, token );

        // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
        User user = findUserByIdNoAuth( userId );

        user.setPassword( bCryptPasswordEncoder.encode( passwordReset.getNewPassword() ) );

        passwordResetTokenRepository.delete( passToken );

        return updateNoAuth( user );
    }

    @Transactional
    @Override
    public VerificationToken createVerificationTokenForUser( User user ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.setEmail( user.getEmail() );
        userToken.updateToken( UUID.randomUUID().toString() );
        return tokenRepository.save( userToken );
    }

    @Override
    @Transactional
    public User confirmVerificationToken( String token ) throws TokenException {
        VerificationToken verificationToken = tokenRepository.findByToken( token );
        if ( verificationToken == null ) {
            throw new TokenException( "Verification token is invalid." );
        }

        if ( Instant.now().isAfter( verificationToken.getExpiryDate().toInstant() ) ) {
            throw new TokenException( "Verification token is expired." );
        }

        User user = verificationToken.getUser();

        if ( !verificationToken.getEmail().equals( user.getEmail() ) ) {
            throw new TokenException( "Verification token does not match user email." );
        }

        tokenRepository.delete( verificationToken );

        user.setEnabled( true );
        return updateNoAuth( user );
    }

    @PostFilter("hasPermission(filterObject, 'read')")
    private Collection<User> findAllWithNonEmptyProfileLastName() {
        return userRepository.findAllWithNonEmptyProfileLastName();
    }

    @Override
    public SortedSet<String> getLastNamesFirstChar() {
        return findAllWithNonEmptyProfileLastName()
                .stream()
                .map( u -> u.getProfile().getLastName().substring( 0, 1 ).toUpperCase() )
                .collect( Collectors.toCollection( TreeSet::new ) );
    }

    @Transactional
    protected User updateNoAuth( User user ) {
        return userRepository.save( user );
    }

    private Set<UserTerm> convertTermTypes( User user, Collection<GeneOntologyTerm> goTerms, Taxon taxon ) {
        return goTerms.stream()
                .map( term -> UserTerm.createUserTerm( user, term, taxon ) )
                .filter( term -> term.getSizeInTaxon( term.getTaxon() ) <= applicationSettings.getGoTermSizeLimit() )
                .collect( Collectors.toSet() );
    }

    @Override
    public void updateUserTerms() {
        log.info( "Updating user terms..." );
        for ( User user : userRepository.findAllWithUserTerms() ) {
            for ( UserTerm userTerm : user.getUserTerms() ) {
                GeneOntologyTerm cachedTerm = goService.getTerm( userTerm.getGoId() );
                if ( cachedTerm == null ) {
                    log.warn( MessageFormat.format( "User has a reference to a GO term missing from the cache: {0}.", userTerm ) );
                    continue;
                }
                userTerm.updateTerm( cachedTerm );
            }
            userRepository.save( user );
        }
        log.info( "Done updating user terms." );
    }

}
