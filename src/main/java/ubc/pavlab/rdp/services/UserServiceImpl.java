package ubc.pavlab.rdp.services;

import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.util.Assert;
import ubc.pavlab.rdp.events.OnContactEmailUpdateEvent;
import ubc.pavlab.rdp.events.OnRegistrationCompleteEvent;
import ubc.pavlab.rdp.events.OnRequestAccessEvent;
import ubc.pavlab.rdp.events.OnUserPasswordResetEvent;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.UserOntologyTerm;
import ubc.pavlab.rdp.repositories.*;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.CacheUtils;
import ubc.pavlab.rdp.util.CollectionUtils;

import javax.validation.ValidationException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static ubc.pavlab.rdp.util.CollectionUtils.containsAtLeastOne;
import static ubc.pavlab.rdp.util.CollectionUtils.nullOrContainsAtLeastOne;

/**
 * Created by mjacobson on 16/01/18.
 */
@Service("userService")
@Transactional(readOnly = true)
@CommonsLog
public class UserServiceImpl implements UserService, InitializingBean {

    static final String
            USERS_BY_ANONYMOUS_ID_CACHE_NAME = "ubc.pavlab.rdp.model.User.byAnonymousId",
            USER_GENES_BY_ANONYMOUS_ID_CACHE_NAME = "ubc.pavlab.rdp.model.UserGene.byAnonymousId";

    @Autowired
    private ApplicationSettings applicationSettings;
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
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private AccessTokenRepository accessTokenRepository;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private GeneInfoService geneInfoService;
    @Autowired
    private PrivacyService privacyService;
    @Autowired
    private SecureRandom secureRandom;
    @Autowired
    private OntologyService ontologyService;

    private Cache usersByAnonymousIdCache;
    private Cache userGenesByAnonymousIdCache;

    @Override
    public void afterPropertiesSet() {
        usersByAnonymousIdCache = CacheUtils.getCache( cacheManager, USERS_BY_ANONYMOUS_ID_CACHE_NAME );
        userGenesByAnonymousIdCache = CacheUtils.getCache( cacheManager, USER_GENES_BY_ANONYMOUS_ID_CACHE_NAME );
        Integer userId = applicationSettings.getIsearch().getUserId();
        if ( userId != null ) {
            User user = userRepository.findById( userId ).orElse( null );
            Assert.notNull( user, String.format( "The remote search user with ID %d does not exist.", userId ) );
            Assert.notEmpty( applicationSettings.getIsearch().getAuthTokens(), "There must be at least one authentication token configured in 'rdp.settings.isearch.auth-tokens'." );
            log.info( String.format( "Using %s for performing remote admin search.", user ) );
        }
    }

    @Transactional
    @Override
    public User create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( "ROLE_USER" );
        user.getRoles().add( userRole );
        return userRepository.save( user );
    }

    @Secured("ROLE_ADMIN")
    @Transactional
    @Override
    public User createAdmin( User admin ) {
        admin.setPassword( bCryptPasswordEncoder.encode( admin.getPassword() ) );
        Role adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        admin.getRoles().add( adminRole );
        return userRepository.save( admin );
    }

    @Override
    @Secured("ROLE_ADMIN")
    @Transactional
    public User createServiceAccount( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( createSecureRandomToken() ) );
        Role serviceAccountRole = roleRepository.findByRole( "ROLE_SERVICE_ACCOUNT" );
        user.getRoles().add( serviceAccountRole );
        user = userRepository.save( user );
        createAccessTokenForUser( user );
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Secured("ROLE_ADMIN")
    @Transactional(rollbackFor = RoleException.class)
    public void updateRoles( User user, Set<Role> roles ) throws RoleException {
        Role adminRole = roleRepository.findByRole( "ROLE_ADMIN" );
        if ( isCurrentUser( user ) && !roles.containsAll( user.getRoles() ) ) {
            throw new CannotRevokeOwnRolesException( "You cannot revoke your own roles." );
        } else if ( user.getRoles().contains( adminRole ) && !roles.containsAll( user.getRoles() ) ) {
            throw new CannotRevokeRolesFromOtherAdministratorException( "You cannot revoke the roles of another administrator." );
        }
        CollectionUtils.update( user.getRoles(), roles );
        userRepository.save( user );
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

    /**
     * Test if the provided user is the current authenticated user.
     * <p>
     * Prefer this over {@link #findCurrentUser()} if you don't need to retrieve the current user and avoid a database
     * roundtrip.
     */
    @Override
    public boolean isCurrentUser( User user ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth == null || auth.getPrincipal().equals( "anonymousUser" ) ) {
            return false;
        }
        return ( (UserPrinciple) auth.getPrincipal() ).getId().equals( user.getId() );
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public User findUserById( int id ) {
        return userRepository.findById( id ).orElse( null );
    }


    /**
     * Retrieve a user using an anonymous identifier.
     * <p>
     * Identifiers are generated and stored when calling anonymizeUser on a {@link User} model.
     * <p>
     * There is no authorization check performed, so make sure that you reuse anonymizeUser before presenting the data
     * in the view.
     *
     * @param anonymousId
     * @return the user if found, otherwise null
     */
    @Override
    public User findUserByAnonymousIdNoAuth( UUID anonymousId ) {
        return usersByAnonymousIdCache.get( anonymousId, User.class );
    }

    /**
     * Retrieve a user's gene using an anonymous identifier.
     * <p>
     * Identifiers are generated and stored when calling anonymizeUserGene on a {@link UserGene} model.
     * <p>
     * There is no authorization check performed, so make sure that you reuse anonymizeUserGene before presenting the
     * data in the view.
     *
     * @param anonymousId
     * @return the user's gene if found, otherwise null
     */
    @Override
    public UserGene findUserGeneByAnonymousIdNoAuth( UUID anonymousId ) {
        return userGenesByAnonymousIdCache.get( anonymousId, UserGene.class );
    }

    @Override
    public User findUserByIdNoAuth( int id ) {
        // Only use this in placed where no authentication of user is needed
        return userRepository.findOneWithRoles( id ).orElse( null );
    }

    @Override
    public User findUserByEmailNoAuth( String email ) {
        return userRepository.findByEmailIgnoreCase( email );
    }

    @Override
    public User findUserByAccessTokenNoAuth( String accessToken ) throws TokenException {
        AccessToken token = accessTokenRepository.findByToken( accessToken );
        if ( token == null ) {
            return null;
        }
        if ( Instant.now().isAfter( token.getExpiryDate().toInstant() ) ) {
            // token is expired
            throw new ExpiredTokenException( "Token is expired." );
        }
        return token.getUser();
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public User anonymizeUser( User user ) {
        return anonymizeUser( user, UUID.randomUUID() );
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public User anonymizeUser( User user, UUID anonymousId ) {
        String shortName = messageSource.getMessage( "rdp.site.shortname", null, Locale.getDefault() );
        Profile profile = Profile.builder()
                .name( messageSource.getMessage( "rdp.site.anonymized-user-name", new String[]{ shortName }, Locale.getDefault() ) )
                .privacyLevel( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ? PrivacyLevelType.PUBLIC : PrivacyLevelType.PRIVATE )
                .shared( true )
                .build();
        profile.getResearcherCategories().addAll( user.getProfile().getResearcherCategories() );
        User anonymizedUser = User.builder( profile )
                .id( null )
                .anonymousId( UUID.randomUUID() )
                // FIXME: a disabled user will still cause an AccessDeniedException
                .enabled( user.isEnabled() )
                .build();
        // TODO: check if this is leaking too much personal information
        anonymizedUser.getUserOrgans().putAll( user.getUserOrgans() );
        usersByAnonymousIdCache.putIfAbsent( anonymizedUser.getAnonymousId(), user );
        return anonymizedUser;
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public UserGene anonymizeUserGene( UserGene userGene ) {
        return anonymizeUserGene( userGene, UUID.randomUUID() );
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public UserGene anonymizeUserGene( UserGene userGene, UUID anonymousIdToReuse ) {
        UserGene anonymizedUserGene = UserGene.builder( anonymizeUser( userGene.getUser() ) )
                .id( null )
                .anonymousId( UUID.randomUUID() )
                .geneInfo( userGene.getGeneInfo() )
                .privacyLevel( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ? PrivacyLevelType.PUBLIC : PrivacyLevelType.PRIVATE )
                .tier( userGene.getTier() )
                .build();
        anonymizedUserGene.updateGene( userGene );
        userGenesByAnonymousIdCache.putIfAbsent( anonymizedUserGene.getAnonymousId(), userGene );
        return anonymizedUserGene;
    }

    @Override
    public void revokeAccessToken( AccessToken accessToken ) {
        accessTokenRepository.delete( accessToken );
    }

    @Override
    public AccessToken createAccessTokenForUser( User user ) {
        AccessToken token = new AccessToken();
        token.updateToken( createSecureRandomToken() );
        token.setUser( user );
        return accessTokenRepository.save( token );
    }

    @Override
    @Cacheable("ubc.pavlab.rdp.services.UserService.remoteSearchUser")
    public Optional<User> getRemoteSearchUser() {
        return Optional.ofNullable( applicationSettings.getIsearch().getUserId() )
                .flatMap( userRepository::findOneWithRoles );
    }

    /**
     * Results are ored
     *
     * @param pageable
     * @return
     */
    @Override
    public Page<User> findAllNoAuth( Pageable pageable ) {
        return userRepository.findAll( pageable );
    }

    @Override
    public Page<User> findByEnabledTrueNoAuth( Pageable pageable ) {
        return userRepository.findByEnabledTrue( pageable );
    }

    @Override
    public Page<User> findByEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType privacyLevel, Pageable pageable ) {
        return userRepository.findByEnabledTrueAndProfilePrivacyLevel( privacyLevel, pageable );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public List<User> findByLikeName( String nameLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<OrganInfo> organs, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        final Set<String> organUberonIds = organUberonIdsFromOrgans( organs );
        Map<Ontology, Set<Integer>> ontologyTermInfoIds = ontologyTermInfoIdsFromOntologyTermInfo( ontologyTermInfos );
        return userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( nameLike, nameLike ).stream()
                .filter( u -> researcherPositions == null || researcherPositions.contains( u.getProfile().getResearcherPosition() ) )
                .filter( u -> nullOrContainsAtLeastOne( researcherTypes, () -> u.getProfile().getResearcherCategories() ) )
                .filter( u -> nullOrContainsAtLeastOne( organUberonIds, () -> u.getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                .filter( hasOntologyTermIn( ontologyTermInfoIds ) )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public List<User> findByStartsName( String startsName, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Collection<OrganInfo> organs, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        final Set<String> organUberonIds = organUberonIdsFromOrgans( organs );
        Map<Ontology, Set<Integer>> ontologyTermInfoIds = ontologyTermInfoIdsFromOntologyTermInfo( ontologyTermInfos );
        return userRepository.findByProfileLastNameStartsWithIgnoreCase( startsName ).stream()
                .filter( u -> researcherPositions == null || researcherPositions.contains( u.getProfile().getResearcherPosition() ) )
                .filter( u -> nullOrContainsAtLeastOne( researcherTypes, () -> u.getProfile().getResearcherCategories() ) )
                .filter( u -> nullOrContainsAtLeastOne( organUberonIds, () -> u.getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                .filter( hasOntologyTermIn( ontologyTermInfoIds ) )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public List<User> findByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<OrganInfo> organs, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        final Set<String> organUberonIds = organUberonIdsFromOrgans( organs );
        Map<Ontology, Set<Integer>> ontologyTermInfoIds = ontologyTermInfoIdsFromOntologyTermInfo( ontologyTermInfos );
        return userRepository.findDistinctByProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCaseOrUserOntologyTermsNameLikeIgnoreCase( "%" + descriptionLike + "%" ).stream()
                .filter( u -> researcherPositions == null || researcherPositions.contains( u.getProfile().getResearcherPosition() ) )
                .filter( u -> nullOrContainsAtLeastOne( researcherTypes, () -> u.getProfile().getResearcherCategories() ) )
                .filter( u -> nullOrContainsAtLeastOne( organUberonIds, () -> u.getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                .filter( hasOntologyTermIn( ontologyTermInfoIds ) )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public List<User> findByNameAndDescription( String nameLike, boolean prefix, String descriptionLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Collection<OrganInfo> organs, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        final Set<String> organUberonIds = organUberonIdsFromOrgans( organs );
        String namePattern = prefix ? nameLike + "%" : "%" + nameLike + "%";
        String descriptionPattern = "%" + descriptionLike + "%";
        List<User> users;
        if ( prefix ) {
            users = userRepository.findDistinctByProfileLastNameLikeIgnoreCaseAndProfileDescriptionLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCase( namePattern, descriptionPattern );
        } else {
            users = userRepository.findDistinctByProfileFullNameLikeIgnoreCaseAndProfileDescriptionLikeIgnoreCaseAndTaxonDescriptionsLikeIgnoreCaseOrTaxonDescriptionsLikeIgnoreCase( namePattern, descriptionPattern );
        }
        Map<Ontology, Set<Integer>> ontologyTermInfoIds = ontologyTermInfoIdsFromOntologyTermInfo( ontologyTermInfos );
        return users.stream()
                .filter( u -> researcherPositions == null || researcherPositions.contains( u.getProfile().getResearcherPosition() ) )
                .filter( u -> nullOrContainsAtLeastOne( researcherCategories, () -> u.getProfile().getResearcherCategories() ) )
                .filter( u -> nullOrContainsAtLeastOne( organUberonIds, () -> u.getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                .filter( hasOntologyTermIn( ontologyTermInfoIds ) )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    private Set<String> organUberonIdsFromOrgans( Collection<OrganInfo> organs ) {
        if ( organs != null ) {
            return organs.stream().map( Organ::getUberonId ).collect( Collectors.toSet() );
        } else {
            return null;
        }
    }

    private Map<Ontology, Set<Integer>> ontologyTermInfoIdsFromOntologyTermInfo( Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        if ( ontologyTermInfos != null ) {
            return ontologyTermInfos.entrySet().stream()
                    .collect( Collectors.toMap( Map.Entry::getKey, e -> ontologyService.inferTermIds( e.getValue() ) ) );
        } else {
            return null;
        }
    }

    @Override
    public Predicate<User> hasOntologyTermIn( Map<Ontology, Set<Integer>> ontologyTermInfoIdsByOntology ) {
        return u -> ontologyTermInfoIdsByOntology == null || ontologyTermInfoIdsByOntology.values().stream()
                .allMatch( entry -> containsAtLeastOne( entry, () -> getUserTermInfoIds( u ) ) );
    }

    @Override
    public boolean existsByOntology( Ontology ontology ) {
        return userRepository.existsByUserOntologyTermsOntology( ontology );
    }

    @Override
    public Set<Integer> getUserTermInfoIds( User user ) {
        return user.getUserOntologyTerms().stream()
                .map( UserOntologyTerm::getTermInfo )
                .filter( Objects::nonNull )
                .filter( OntologyTermInfo::isActive ) // check this first, otherwise we might initialize the ontology for nothing
                .filter( t -> t.getOntology().isActive() )
                .map( OntologyTermInfo::getId )
                .collect( Collectors.toSet() );
    }

    @Override
    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    public long countResearchers() {
        return userRepository.countByEnabledTrue();
    }

    @Override
    public long countPublicResearchers() {
        return userRepository.countByEnabledTrueAndProfilePrivacyLevel( PrivacyLevelType.PUBLIC );
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public UserTerm convertTerm( User user, Taxon taxon, GeneOntologyTermInfo term ) {
        UserTerm ut = UserTerm.createUserTerm( user, term, taxon );
        ut.setFrequency( computeTermFrequencyInTaxon( user, term, taxon ) );
        ut.setSize( goService.getSizeInTaxon( term, taxon ) );
        return ut;
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTermInfo> terms ) {
        return terms.stream()
                .map( term -> convertTerm( user, taxon, term ) )
                .collect( Collectors.toSet() );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserTerm> recommendTerms( @NonNull User user, @NonNull Taxon taxon ) {
        return recommendTerms( user, user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ), taxon, 10, applicationSettings.getGoTermSizeLimit(), 2 );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserTerm> recommendTerms( User user, Set<? extends Gene> genes, Taxon taxon ) {
        return recommendTerms( user, genes, taxon, 10, applicationSettings.getGoTermSizeLimit(), 2 );
    }


    /**
     * This is only meant for testing purposes; refrain from using in actual code.
     */
    @PostFilter("hasPermission(filterObject, 'read')")
    Collection<UserTerm> recommendTerms( @NonNull User user, @NonNull Taxon taxon, long minSize, long maxSize, long minFrequency ) {
        return recommendTerms( user, user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ), taxon, minSize, maxSize, minFrequency );
    }

    private Collection<UserTerm> recommendTerms( @NonNull User user, Set<? extends Gene> genes, @NonNull Taxon taxon, long minSize, long maxSize, long minFrequency ) {
        // terms already associated to user within the taxon
        Set<String> userTermGoIds = user.getUserTerms().stream()
                .filter( ut -> ut.getTaxon().equals( taxon ) )
                .map( UserTerm::getGoId )
                .collect( Collectors.toSet() );

        // Then keep only those terms not already added and with the highest frequency
        Set<GeneOntologyTermInfo> topResults = goService.termFrequencyMap( genes ).entrySet().stream()
                .filter( e -> minFrequency < 0 || e.getValue() >= minFrequency )
                .filter( e -> minSize < 0 || goService.getSizeInTaxon( e.getKey(), taxon ) >= minSize )
                .filter( e -> maxSize < 0 || goService.getSizeInTaxon( e.getKey(), taxon ) <= maxSize )
                .filter( e -> !userTermGoIds.contains( e.getKey().getGoId() ) )
                .map( Map.Entry::getKey )
                .collect( Collectors.toSet() );

        // Keep only leafiest of remaining terms (keep if it has no descendants in results)
        return topResults.stream()
                .filter( term -> Collections.disjoint( topResults, goService.getDescendants( term ) ) )
                .filter( term -> goService.getSizeInTaxon( term, taxon ) <= applicationSettings.getGoTermSizeLimit() )
                .map( term -> convertTerm( user, taxon, term ) )
                .collect( Collectors.toSet() );
    }

    @Transactional
    @Override
    @PreAuthorize("hasPermission(#user, 'update')")
    public User updateTermsAndGenesInTaxon( User user,
                                            Taxon taxon,
                                            Map<GeneInfo, TierType> genesToTierMap,
                                            Map<GeneInfo, PrivacyLevelType> genesToPrivacyLevelMap,
                                            Collection<GeneOntologyTermInfo> goTerms ) {
        Map<Integer, UserGene> userGenes = genesToTierMap.keySet()
                .stream()
                .filter( g -> g.getTaxon().equals( taxon ) )
                .map( g -> {
                    UserGene userGene = user.getUserGenes().getOrDefault( g.getGeneId(), new UserGene() );
                    userGene.setUser( user );
                    userGene.setTier( genesToTierMap.get( g ) );
                    if ( applicationSettings.getPrivacy().isCustomizableGeneLevel() ) {
                        // if no privacy level is set, we inherit the profile value
                        PrivacyLevelType privacyLevel = genesToPrivacyLevelMap.getOrDefault( g, null );
                        if ( privacyLevel == null || privacyService.isGenePrivacyLevelEnabled( privacyLevel ) ) {
                            userGene.setPrivacyLevel( privacyLevel );
                        } else {
                            log.warn( MessageFormat.format( "{0} attempted to set {1} privacy level to a value that is not enabled: {2}. The new value was ignored.", findCurrentUser(), g, privacyLevel ) );
                        }
                    }
                    userGene.updateGene( g );
                    return userGene;
                } )
                .collect( Collectors.toMap( Gene::getGeneId, identity() ) );

        // add calculated genes from terms
        Map<Integer, UserGene> userGenesFromTerms = goTerms.stream()
                .flatMap( term -> goService.getGenesInTaxon( term, taxon ).stream() )
                .distinct() // terms might refer to the same gene
                .map( geneInfoService::load )
                .filter( Objects::nonNull )
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
        Collection<UserTerm> userTerms = convertTerms( user, taxon, goTerms );
        CollectionUtils.updateIf( user.getUserTerms(), userTerms, e -> e.getTaxon().equals( taxon ) );

        // update frequency and size as those have likely changed with new genes
        for ( UserTerm userTerm : user.getUserTerms() ) {
            GeneOntologyTermInfo cachedTerm = goService.getTerm( userTerm.getGoId() );
            userTerm.setFrequency( computeTermFrequencyInTaxon( user, cachedTerm, taxon ) );
            userTerm.setSize( goService.getSizeInTaxon( cachedTerm, taxon ) );
        }

        return update( user );
    }

    @Override
    public long computeTermOverlaps( UserTerm userTerm, Collection<GeneInfo> genes ) {
        return genes.stream()
                .flatMap( g -> goService.getTermsForGene( g, true ).stream() )
                .filter( term -> term.getGoId().equals( userTerm.getGoId() ) )
                .count();
    }

    /**
     * Compute the number of TIER1/TIER2 user genes that are associated to a given ontology term.
     *
     * @param user
     * @param term
     * @param taxon
     * @return
     */
    @Override
    public long computeTermFrequencyInTaxon( User user, GeneOntologyTerm term, Taxon taxon ) {
        Set<Integer> geneIds = new HashSet<>( goService.getGenes( goService.getTerm( term.getGoId() ) ) );
        return user.getGenesByTaxonAndTier( taxon, TierType.MANUAL ).stream()
                .map( UserGene::getGeneId )
                .filter( geneIds::contains )
                .count();
    }

    @Override
    @Transactional
    public void sendGeneAccessRequest( User requestingUser, UserGene userGene, String reason ) {
        eventPublisher.publishEvent( new OnRequestAccessEvent<>( requestingUser, userGene, reason ) );
    }

    @Transactional
    @Override
    @PreAuthorize("hasPermission(#user, 'update')")
    public User updateUserProfileAndPublicationsAndOrgansAndOntologyTerms( User user, Profile profile, Set<Publication> publications, Set<String> organUberonIds, Set<Integer> termIdsByOntologyId, Locale locale ) {
        user.getProfile().setDepartment( profile.getDepartment() );
        user.getProfile().setDescription( profile.getDescription() );
        user.getProfile().setLastName( profile.getLastName() );
        user.getProfile().setName( profile.getName() );
        if ( profile.getResearcherPosition() == null || applicationSettings.getProfile().getEnabledResearcherPositions().contains( profile.getResearcherPosition() ) ) {
            user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        } else {
            log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher position to an unknown value {2}.",
                    findCurrentUser(), user, profile.getResearcherPosition() ) );
        }
        user.getProfile().setResearcherPosition( profile.getResearcherPosition() );
        user.getProfile().setOrganization( profile.getOrganization() );

        if ( profile.getResearcherCategories() != null ) {
            if ( applicationSettings.getProfile().getEnabledResearcherCategories().containsAll( profile.getResearcherCategories() ) ) {
                user.getProfile().getResearcherCategories().retainAll( profile.getResearcherCategories() );
                user.getProfile().getResearcherCategories().addAll( profile.getResearcherCategories() );
            } else {
                log.warn( MessageFormat.format( "User {0} attempted to set user {1} researcher type to an unknown value {2}.",
                        findCurrentUser(), user, profile.getResearcherCategories() ) );
            }
        }

        if ( user.getProfile().getContactEmail() == null ||
                !user.getProfile().getContactEmail().equals( profile.getContactEmail() ) ) {
            user.getProfile().setContactEmail( profile.getContactEmail() );
            if ( user.getProfile().getContactEmail() != null && !user.getProfile().getContactEmail().isEmpty() ) {
                if ( user.getProfile().getContactEmail().equals( user.getEmail() ) ) {
                    // if the contact email is set to the user email, it's de facto verified
                    user.getProfile().setContactEmailVerified( true );
                    user.getProfile().setContactEmailVerifiedAt( user.getEnabledAt() );
                } else {
                    user.getProfile().setContactEmailVerified( false );
                    user.getProfile().setContactEmailVerifiedAt( null );
                    VerificationToken token = createContactEmailVerificationTokenForUser( user, locale );
                }
            } else {
                // contact email is unset, so we don't need to send a confirmation
                user.getProfile().setContactEmailVerified( false );
                user.getProfile().setContactEmailVerifiedAt( null );
            }
        }

        user.getProfile().setPhone( profile.getPhone() );
        user.getProfile().setWebsite( profile.getWebsite() );

        // privacy settings
        if ( applicationSettings.getPrivacy().isCustomizableLevel() ) {
            if ( privacyService.isPrivacyLevelEnabled( profile.getPrivacyLevel() ) ) {
                // reset gene privacy levels if the profile value is changed
                if ( applicationSettings.getPrivacy().isCustomizableGeneLevel() &&
                        user.getProfile().getPrivacyLevel() != profile.getPrivacyLevel() ) {
                    // reset gene-level privacy when profile is updated
                    user.getUserGenes().values().forEach( ug -> ug.setPrivacyLevel( profile.getPrivacyLevel() ) );
                }
                user.getProfile().setPrivacyLevel( profile.getPrivacyLevel() );
            } else {
                log.warn( MessageFormat.format( "{0} attempted to set {1} its profile privacy level to a disabled value: {2}. The new value was ignored.",
                        findCurrentUser(), user, profile.getPrivacyLevel() ) );
            }
        }
        if ( applicationSettings.getPrivacy().isCustomizableSharing() ) {
            user.getProfile().setShared( profile.isShared() );
        }
        if ( applicationSettings.getPrivacy().isAllowHideGenelist() ) {
            user.getProfile().setHideGenelist( profile.isHideGenelist() );
        }

        if ( publications != null ) {
            user.getProfile().getPublications().retainAll( publications );
            user.getProfile().getPublications().addAll( publications );
        }

        if ( applicationSettings.getOrgans().isEnabled() ) {
            Map<String, UserOrgan> userOrgans = organInfoService.findByUberonIdIn( organUberonIds ).stream()
                    .map( organInfo -> user.getUserOrgans().getOrDefault( organInfo.getUberonId(), UserOrgan.createFromOrganInfo( user, organInfo ) ) )
                    .collect( Collectors.toMap( Organ::getUberonId, identity() ) );
            user.getUserOrgans().clear();
            user.getUserOrgans().putAll( userOrgans );
        }

        if ( termIdsByOntologyId != null ) {
            Set<UserOntologyTerm> userOntologyTerms = new HashSet<>();
            for ( Integer ontologyTermId : termIdsByOntologyId ) {
                OntologyTermInfo termInfo = ontologyService.findTermById( ontologyTermId );
                if ( termInfo == null ) {
                    log.warn( String.format( "Unknown term with ID %d.", ontologyTermId ) );
                    continue;
                }
                userOntologyTerms.add( UserOntologyTerm.fromOntologyTermInfo( user, termInfo ) );
            }
            CollectionUtils.update( user.getUserOntologyTerms(), userOntologyTerms );
        }

        return update( user );
    }

    @Transactional
    @Override
    public PasswordResetToken createPasswordResetTokenForUser( User user, Locale locale ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( createSecureRandomToken() );
        userToken = passwordResetTokenRepository.save( userToken );
        eventPublisher.publishEvent( new OnUserPasswordResetEvent( user, userToken, locale ) );
        return userToken;
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

    @Transactional(rollbackFor = { TokenException.class })
    @Override
    public User changePasswordByResetToken( int userId, String token, PasswordReset passwordReset ) throws TokenException, ValidationException {

        PasswordResetToken passToken = verifyPasswordResetToken( userId, token );

        // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
        User user = findUserByIdNoAuth( userId );

        user.setPassword( bCryptPasswordEncoder.encode( passwordReset.getNewPassword() ) );

        passwordResetTokenRepository.delete( passToken );

        return userRepository.save( user );
    }

    @Transactional
    @Override
    public VerificationToken createVerificationTokenForUser( User user, Locale locale ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.setEmail( user.getEmail() );
        userToken.updateToken( createSecureRandomToken() );
        userToken = tokenRepository.save( userToken );
        eventPublisher.publishEvent( new OnRegistrationCompleteEvent( user, userToken, locale ) );
        return userToken;
    }

    @Transactional
    @Override
    public VerificationToken createContactEmailVerificationTokenForUser( User user, Locale locale ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.setEmail( user.getProfile().getContactEmail() );
        userToken.updateToken( createSecureRandomToken() );
        userToken = tokenRepository.save( userToken );
        eventPublisher.publishEvent( new OnContactEmailUpdateEvent( user, userToken, locale ) );
        return userToken;
    }

    @Override
    @Transactional(rollbackFor = { TokenException.class })
    public User confirmVerificationToken( String token ) throws TokenException {
        VerificationToken verificationToken = tokenRepository.findByToken( token );
        if ( verificationToken == null ) {
            throw new TokenException( "Verification token is invalid." );
        }

        if ( Instant.now().isAfter( verificationToken.getExpiryDate().toInstant() ) ) {
            throw new TokenException( "Verification token is expired." );
        }

        User user = verificationToken.getUser();

        boolean tokenUsed = false;

        if ( verificationToken.getEmail().equals( user.getEmail() ) ) {
            user.setEnabled( true );
            user.setEnabledAt( Timestamp.from( Instant.now() ) );
            tokenUsed = true;
        }

        if ( user.getProfile().getContactEmail() != null && verificationToken.getEmail().equals( user.getProfile().getContactEmail() ) ) {
            user.getProfile().setContactEmailVerified( true );
            user.getProfile().setContactEmailVerifiedAt( Timestamp.from( Instant.now() ) );
            tokenUsed = true;
        }

        if ( tokenUsed ) {
            tokenRepository.delete( verificationToken );
            return userRepository.save( user );
        } else {
            throw new TokenException( "Verification token email does not match neither the user email nor contact email." );
        }
    }

    @Override
    public SortedSet<String> getLastNamesFirstChar() {
        return userRepository.findAllWithNonEmptyProfileLastNameAndProfilePrivacyLevelGreaterOrEqualThan( findCurrentUser() == null ? PrivacyLevelType.PUBLIC : PrivacyLevelType.SHARED )
                .stream()
                .map( u -> u.getProfile().getLastName().substring( 0, 1 ).toUpperCase() )
                .filter( StringUtils::isAlpha )
                .collect( Collectors.toCollection( TreeSet::new ) );
    }

    @Override
    @Transactional
    public void updateUserTerms() {
        log.info( "Updating user terms..." );
        for ( User user : userRepository.findAllWithUserTerms() ) {
            for ( UserTerm userTerm : user.getUserTerms() ) {
                GeneOntologyTermInfo cachedTerm = goService.getTerm( userTerm.getGoId() );
                if ( cachedTerm == null ) {
                    log.warn( MessageFormat.format( "User has a reference to a GO term missing from the cache: {0}.", userTerm ) );
                    continue;
                }
                userTerm.updateTerm( cachedTerm );
                userTerm.setFrequency( computeTermFrequencyInTaxon( user, cachedTerm, userTerm.getTaxon() ) );
                userTerm.setSize( goService.getSizeInTaxon( cachedTerm, userTerm.getTaxon() ) );
            }
            userRepository.save( user );
        }
        log.info( "Done updating user terms." );
    }

    private String createSecureRandomToken() {
        byte[] tokenBytes = new byte[24];
        secureRandom.nextBytes( tokenBytes );
        return Base64.getEncoder().encodeToString( tokenBytes );
    }
}
