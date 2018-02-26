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
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 16/01/18.
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    private static Log log = LogFactory.getLog( UserServiceImpl.class );

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
    ApplicationSettings applicationSettings;

    @Transactional
    @Override
    public User create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( applicationSettings.isDefaultNewUserRoleAsManager() ? "ROLE_MANAGER" : "ROLE_USER" );

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
    private User updateNoAuth( User user ) {
        return userRepository.save( user );
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
    public User changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException {
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

    @Transactional
    @Override
    public User changePasswordByResetToken( int userId, String token, String newPassword ) throws TokenException, ValidationException {

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
        return findUserByIdNoAuth( ((UserPrinciple) auth.getPrincipal()).getId() );
    }

    @Override
    public User findUserById( int id ) {
        return userRepository.findOne( id );
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
    public User findUserByUserName( String email ) {
        return findUserByEmail( email );
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Collection<User> findByLikeName( String nameLike ) {
        return userRepository.findByProfileNameContainingIgnoreCaseOrProfileLastNameContainingIgnoreCase( nameLike, nameLike );
    }

    @Override
    public Collection<User> findByDescription( String descriptionLike ) {
        return userRepository.findByProfileDescriptionContainingIgnoreCaseOrTaxonDescriptionsContainingIgnoreCase( descriptionLike, descriptionLike );
    }

    @Override
    public long countResearchers() {
        return userRepository.count();
    }

    @Override
    public Integer countResearchersWithGenes() {
        return userRepository.countWithGenes();
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

    private Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon, Set<Gene> genes ) {
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

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon ) {
        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return convertTermTypes( goService.recommendTerms( genes ), taxon, genes );
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

    @Transactional
    @Override
    public void updateTermsAndGenesInTaxon( User user, Taxon taxon, Map<Gene, TierType> genesToTierMap, Collection<GeneOntologyTerm> goTerms ) {
        // Remove genes from other taxons (they shouldn't be here but just incase)
        genesToTierMap.keySet().removeIf( e -> !e.getTaxon().equals( taxon ) );
        int initialSize = user.getUserGenes().size();

        // Update terms

        // Inform Hibernate of similar entities
        Map<String, Integer> goIdToHibernateId = user.getUserTerms().stream()
                .filter( t -> t.getTaxon().equals( taxon ) )
                .collect( Collectors.toMap( GeneOntologyTerm::getGoId, UserTerm::getId ) );
        Collection<UserTerm> updatedTerms = convertTerms( user, taxon, goTerms );
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

        int removed = user.getUserGenes().size() - (initialSize + added);

        log.info( "Added: " + added + ", removed: " + removed + ", updated: " + updated + " genes, User " + user.getEmail() );

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
        if ( (passToken == null) || (!passToken.getUser().getId().equals( userId )) ) {
            throw new TokenException( "Invalid Token" );
        }

        Calendar cal = Calendar.getInstance();
        if ( (passToken.getExpiryDate()
                .getTime() - cal.getTime()
                .getTime()) <= 0 ) {
            throw new TokenException( "Expired" );
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
        if ( (verificationToken.getExpiryDate()
                .getTime() - cal.getTime()
                .getTime()) <= 0 ) {
            throw new TokenException( "Expired" );
        }

        User user = verificationToken.getUser();
        user.setEnabled( true );
        updateNoAuth( user );
        return user;
    }

    private Collection<Gene> calculatedGenesInTaxon( User user, Taxon taxon ) {
        return goService.getRelatedGenes( user.getUserTerms(), taxon );
    }

    private boolean removeGenesFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserGenes().values().removeIf( ga -> ga.getTaxon().equals( taxon ) );
    }

    private boolean removeGenesFromUserByTiersAndTaxon( User user, Taxon taxon, Collection<TierType> tiers ) {
        return user.getUserGenes().values().removeIf( ga -> tiers.contains( ga.getTier() ) && ga.getTaxon().equals( taxon ) );
    }

    private boolean removeTermsFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserTerms().removeIf( ut -> ut.getTaxon().equals( taxon ) );
    }

}
