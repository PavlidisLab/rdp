package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.exception.TokenException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.PasswordResetTokenRepository;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.repositories.VerificationTokenRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.validation.ValidationException;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

/**
 * Created by mjacobson on 16/01/18.
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    private static final int MAX_CHARS_SHOW = 26;
    private static Log log = LogFactory.getLog( UserServiceImpl.class );
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
    private Role roleAdmin;

    @SuppressWarnings("unused") // Keeping for future use
    private static <T> Collector<T, ?, List<T>> maxList( Comparator<? super T> comp ) {
        return Collector.of( ArrayList::new, ( list, t ) -> {
            int c;
            if ( list.isEmpty() || ( c = comp.compare( t, list.get( 0 ) ) ) == 0 ) {
                list.add( t );
            } else if ( c > 0 ) {
                list.clear();
                list.add( t );
            }
        }, ( list1, list2 ) -> {
            if ( list1.isEmpty() ) {
                return list2;
            }
            if ( list2.isEmpty() ) {
                return list1;
            }
            int r = comp.compare( list1.get( 0 ), list2.get( 0 ) );
            if ( r < 0 ) {
                return list2;
            } else if ( r > 0 ) {
                return list1;
            } else {
                list1.addAll( list2 );
                return list1;
            }
        } );
    }

    private static <T> Collector<T, ?, Set<T>> maxSet( Comparator<? super T> comp ) {
        return Collector.of( HashSet::new, ( set, t ) -> {
            int c;
            if ( set.isEmpty() || ( c = comp.compare( t, set.iterator().next() ) ) == 0 ) {
                set.add( t );
            } else if ( c > 0 ) {
                set.clear();
                set.add( t );
            }
        }, ( set1, set2 ) -> {
            if ( set1.isEmpty() ) {
                return set2;
            }
            if ( set2.isEmpty() ) {
                return set1;
            }
            int r = comp.compare( set1.iterator().next(), set2.iterator().next() );
            if ( r < 0 ) {
                return set2;
            } else if ( r > 0 ) {
                return set1;
            } else {
                set1.addAll( set2 );
                return set1;
            }
        } );
    }

    @Transactional
    @Override
    public User create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( "ROLE_USER" );

        user.setRoles( Collections.singleton( userRole ) );
        return userRepository.save( user );
    }

    @Transactional
    @Override
    public User update( User user ) {
        // Currently restrict to updating your own user. Can make this better with
        // Pre-Post authorized annotations.
        String currentUsername = getCurrentEmail();
        if ( user.getEmail().equals( currentUsername ) ) {
            return userRepository.save( user );
        } else {
            log.warn( currentUsername + " attempted to update a user that is not their own: " + user.getEmail() );
        }

        return null;

    }

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
        return findUserByIdNoAuth( ( ( UserPrinciple ) auth.getPrincipal() ).getId() );
    }

    @Override
    public User findUserById( int id ) {
        User user = userRepository.findOne( id );
        return user == null ? null : checkCurrentUserCanSee( user ) ? user : null;
    }

    @Override
    public User findUserByIdNoAuth( int id ) {
        // Only use this in placed where no authentication of user is needed
        return userRepository.findOne( id );
    }

    @Override
    public User findUserByEmail( String email ) {
        return userRepository.findByEmailIgnoreCase( email );
    }

    @Override
    public User getRemoteAdmin() {
        return userRepository.findOne( applicationSettings.getIsearch().getUserId() );
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream().filter( this::checkCurrentUserCanSee ).collect( Collectors.toList() );
    }

    @Override
    public Collection<User> findByLikeName( String nameLike ) {
        return securityFilter( userRepository
                .findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( nameLike, nameLike ) );
    }

    @Override
    public Collection<User> findByStartsName( String startsName ) {
        return securityFilter( userRepository
                .findByProfileLastNameStartsWithIgnoreCase( startsName ) );
    }

    @Override
    public Collection<User> findByDescription( String descriptionLike ) {
        return securityFilter( userRepository
                .findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( descriptionLike,
                        descriptionLike ) );
    }

    @Override
    public long countResearchers() {
        return userRepository.count();
    }

    @Override
    public Collection<UserTerm> convertTerms( User user, Taxon taxon, Collection<GeneOntologyTerm> terms ) {
        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return convertTermTypes( terms, taxon, genes );
    }

    @Override
    public UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term ) {
        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return convertTermTypes( term, taxon, genes );
    }

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon ) {
        return recommendTerms( user, taxon, 10, applicationSettings.getGoTermSizeLimit(), 2 );
    }

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon, int minSize, int maxSize, int minFrequency ) {
        if ( user == null || taxon == null )
            return null;

        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        Map<GeneOntologyTerm, Long> fmap = goService.termFrequencyMap( genes );

        Stream<Map.Entry<GeneOntologyTerm, Long>> resultStream = fmap.entrySet().stream();

        // Filter out terms without enough overlap or that are too broad/specific
        if ( minFrequency > 0 ) {
            resultStream = resultStream.filter( e -> e.getValue() >= minFrequency );
        }

        if ( minSize > 0 ) {
            resultStream = resultStream.filter( e -> e.getKey().getSize( taxon ) >= minSize );
        }

        if ( maxSize > 0 ) {
            resultStream = resultStream.filter( e -> e.getKey().getSize( taxon ) <= maxSize );
        }

        Set<UserTerm> userTerms = user.getTermsByTaxon( taxon );

        // Then keep only those terms not already added and with the highest frequency
        Set<UserTerm> topResults = resultStream.map( e -> {
            UserTerm ut = new UserTerm( e.getKey(), taxon, null );
            ut.setFrequency( e.getValue().intValue() );
            return ut;
        } ).filter( ut -> !userTerms.contains( ut ) ).collect( maxSet( comparing( UserTerm::getFrequency ) ) );

        // Keep only leafiest of remaining terms (keep if it has no descendants in results)
        return topResults.stream().filter( ut -> Collections.disjoint( topResults, goService.getDescendants( ut ) ) )
                .collect( Collectors.toSet() );
    }

    @Transactional
    @Override
    public void updateTermsAndGenesInTaxon( User user, Taxon taxon, Map<Gene, TierType> genesToTierMap,
            Collection<GeneOntologyTerm> goTerms ) {
        // Remove genes from other taxons (they shouldn't be here but just incase)
        genesToTierMap.keySet().removeIf( e -> !e.getTaxon().equals( taxon ) );
        int initialSize = user.getUserGenes().size();

        // Update terms

        // Inform Hibernate of similar entities
        Map<String, Integer> goIdToHibernateId = user.getUserTerms().stream()
                .filter( t -> t.getTaxon().equals( taxon ) )
                .collect( Collectors.toMap( GeneOntologyTerm::getGoId, UserTerm::getId ) );
        Collection<UserTerm> updatedTerms = convertTermTypes( goTerms, taxon, genesToTierMap.keySet() );
        updatedTerms.forEach( t -> t.setId( goIdToHibernateId.get( t.getGoId() ) ) );

        removeTermsFromUserByTaxon( user, taxon );
        user.getUserTerms().addAll( updatedTerms );

        for ( Gene gene : calculatedGenesInTaxon( user, taxon ) ) {
            genesToTierMap.putIfAbsent( gene, TierType.TIER3 );
        }

        int updated = 0;
        for ( Map.Entry<Gene, TierType> entry : genesToTierMap.entrySet() ) {
            updated += updateOrInsert( user, entry.getKey(), entry.getValue() ) ? 1 : 0;
        }

        int added = user.getUserGenes().size() - initialSize;

        // Remove genes that no longer belong in this taxon
        user.getUserGenes().values().removeIf( g -> g.getTaxon().equals( taxon ) && !genesToTierMap.containsKey( g ) );

        int removed = user.getUserGenes().size() - ( initialSize + added );

        log.info( "Added: " + added + ", removed: " + removed + ", updated: " + updated + " genes, User " + user
                .getEmail() );

        update( user );
    }

    @Transactional
    @Override
    public User updatePublications( User user, Set<Publication> publications ) {
        user.getProfile().getPublications().retainAll( publications );
        user.getProfile().getPublications().addAll( publications );
        return update( user );
    }

    @Transactional
    @Override
    public PasswordResetToken createPasswordResetTokenForUser( User user, String token ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        return passwordResetTokenRepository.save( userToken );
    }

    @Override
    public void verifyPasswordResetToken( int userId, String token ) throws TokenException {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken( token );
        if ( ( passToken == null ) || ( !passToken.getUser().getId().equals( userId ) ) ) {
            throw new TokenException( "Invalid Token" );
        }

        Calendar cal = Calendar.getInstance();
        if ( ( passToken.getExpiryDate().getTime() - cal.getTime().getTime() ) <= 0 ) {
            throw new TokenException( "Expired" );
        }
    }

    @Transactional
    @Override
    public User changePasswordByResetToken( int userId, String token, String newPassword )
            throws TokenException, ValidationException {

        verifyPasswordResetToken( userId, token );

        if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

            // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
            User user = findUserByIdNoAuth( userId );

            user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
            return updateNoAuth( user );
        } else {
            throw new ValidationException( "Password must be a minimum of 6 characters" );
        }
    }

    @Transactional
    @Override
    public VerificationToken createVerificationTokenForUser( User user, String token ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        return tokenRepository.save( userToken );
    }

    @Transactional
    @Override
    public User confirmVerificationToken( String token ) {
        VerificationToken verificationToken = tokenRepository.findByToken( token );
        if ( verificationToken == null ) {
            throw new TokenException( "Invalid Token" );
        }

        Calendar cal = Calendar.getInstance();
        if ( ( verificationToken.getExpiryDate().getTime() - cal.getTime().getTime() ) <= 0 ) {
            throw new TokenException( "Expired" );
        }

        User user = verificationToken.getUser();
        user.setEnabled( true );
        updateNoAuth( user );
        return user;
    }

    @Override
    public boolean checkCurrentUserCanSee( User user ) {
        User currentUser = findCurrentUser();
        if ( roleAdmin == null ) {
            roleAdmin = roleRepository.findByRole( "ROLE_ADMIN" );
        }

        // Never show the remote admin profile (or accidental null users)
        if ( user == null || ( applicationSettings.getIsearch() != null && user.getId()
                .equals( applicationSettings.getIsearch().getUserId() ) ) ) {
            return false;
        }

        Profile profile = user.getProfile();

        if ( profile == null || profile.getPrivacyLevel() == null || profile.getShared() == null ) {
            log.error( "!! User without a profile, privacy levels or sharing set: " + user.getId() + " / " + user
                    .getEmail() );
            return false;
        }

        // Either the user is looking at himself, or the user is public, or shared with registered users - check for any logged-in user, or private - check for admin; If logged-in user is admin, we have to
        // check whether this user is the designated actor for the authenticated remote search, in which case we have to check for remote search privileges on the user.
        return user.equals( currentUser ) // User is looking at himself
                || ( profile.getPrivacyLevel().equals( PRIVACY_PUBLIC ) ) // Data is public
                || ( profile.getPrivacyLevel().equals( PRIVACY_REGISTERED ) && currentUser != null && !currentUser
                .getId().equals( applicationSettings.getIsearch().getUserId() ) )
                // data is accessible for registerd users and there is a user logged in who is not the remote admin
                || ( profile.getPrivacyLevel().equals( PRIVACY_PRIVATE ) && currentUser != null && currentUser
                .getRoles().contains( roleAdmin ) && !currentUser.getId()
                .equals( applicationSettings.getIsearch().getUserId() ) )
                // data is private and there is an admin logged in who is not the remote admin
                || ( profile.getShared() && currentUser != null && currentUser.getRoles().contains( roleAdmin )
                && currentUser.getId().equals( applicationSettings.getIsearch()
                .getUserId() ) ); // data is designated as remotely shared and there is an admin logged in who is the remote admin
    }

    @Override
    public boolean checkCurrentUserCanSee( UserGene userGene ) {
        return checkCurrentUserCanSee( userGene.getUser() );
    }

    @Override
    public List<String> getChars() {
        List<User> users = this.findAll();
        Set<String> chars = new HashSet<>();
        for ( User u : users ) {
            if ( checkCurrentUserCanSee( u ) ) {
                if ( u.getProfile().getName() != null && !u.getProfile().getName().isEmpty() )
                    chars.add( u.getProfile().getName().substring( 0, 1 ).toUpperCase() );
                if ( u.getProfile().getLastName() != null && !u.getProfile().getLastName().isEmpty() )
                    chars.add( u.getProfile().getLastName().substring( 0, 1 ).toUpperCase() );
            }
            if ( chars.size() >= MAX_CHARS_SHOW )
                break;
        }
        List<String> sorted = new ArrayList<>( chars );
        sorted.sort( String.CASE_INSENSITIVE_ORDER );
        return sorted;
    }

    @Transactional
    protected User updateNoAuth( User user ) {
        return userRepository.save( user );
    }

    private Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon,
            Set<Gene> genes ) {
        List<UserTerm> newTerms = new ArrayList<>();
        for ( GeneOntologyTerm goTerm : goTerms ) {
            UserTerm term = convertTermTypes( goTerm, taxon, genes );
            if ( term != null ) {
                newTerms.add( term );
            }
        }
        return newTerms;
    }

    private UserTerm convertTermTypes( GeneOntologyTerm goTerm, Taxon taxon, Set<Gene> genes ) {
        if ( goTerm != null ) {
            UserTerm term = new UserTerm( goTerm, taxon, genes );
            if ( term.getSize() <= applicationSettings.getGoTermSizeLimit() ) {
                return term;
            }
        }

        return null;
    }

    private boolean updateOrInsert( User user, Gene gene, TierType tier ) {
        if ( user == null || gene == null || tier == null ) {
            return false;
        }

        UserGene existing = user.getUserGenes().get( gene.getGeneId() );

        boolean updated = false;
        if ( existing != null ) {
            // Only set tier because the rest of it's information is updated PreLoad
            updated = !existing.getTier().equals( tier );
            existing.setTier( tier );
        } else {
            user.getUserGenes().put( gene.getGeneId(), new UserGene( gene, user, tier ) );
        }

        return updated;
    }

    private Collection<Gene> calculatedGenesInTaxon( User user, Taxon taxon ) {
        return goService.getGenes( user.getTermsByTaxon( taxon ), taxon );
    }

    private boolean removeGenesFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserGenes().values().removeIf( ga -> ga.getTaxon().equals( taxon ) );
    }

    private boolean removeGenesFromUserByTiersAndTaxon( User user, Taxon taxon, Collection<TierType> tiers ) {
        return user.getUserGenes().values()
                .removeIf( ga -> tiers.contains( ga.getTier() ) && ga.getTaxon().equals( taxon ) );
    }

    private boolean removeTermsFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserTerms().removeIf( ut -> ut.getTaxon().equals( taxon ) );
    }

    private Collection<User> securityFilter( Collection<User> users ) {
        return users.stream().filter( this::checkCurrentUserCanSee ).collect( Collectors.toList() );
    }

}
