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

    @Transactional
    @Override
    public void create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        Role userRole = roleRepository.findByRole( "ROLE_USER" );

        user.setRoles( Collections.singleton( userRole ) );
        userRepository.save( user );
    }

    @Transactional
    @Override
    public void update( User user ) {
        userRepository.save( user );
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
    public void changePassword( String oldPassword, String newPassword ) throws BadCredentialsException, ValidationException {
        User user = findCurrentUser();
        if ( bCryptPasswordEncoder.matches( oldPassword, user.getPassword() ) ) {
            if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

                user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
                update( user );
            } else {
                throw new ValidationException( "Password must be a minimum of 6 characters" );
            }
        } else {
            throw new BadCredentialsException( "Password incorrect" );
        }
    }

    @Transactional
    @Override
    public void changePasswordByResetToken( int userId, String token, String newPassword ) throws TokenException, ValidationException {

        verifyPasswordResetToken( userId, token );

        if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

            // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
            User user = findUserById( userId );

            user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
            update( user );
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
        return findUserByEmail( getCurrentEmail() );
    }

    @Override
    public User findUserById( int id ) {
        return userRepository.findOne( id );
    }

    @Override
    public User findUserByEmail( String email ) {
        return userRepository.findByEmail( email );
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
    public Collection<User> findByGene( Gene gene ) {
        return userRepository.findByGene( gene );
    }

    @Override
    public Collection<User> findByGene( Gene gene, TierType tier ) {
        return userRepository.findByGene( gene, tier );
    }

    @Override
    public Collection<User> findByLikeSymbol( String symbol, Taxon taxon ) {
        return userRepository.findByGeneSymbolLike( symbol, taxon );
    }

    @Override
    public Collection<User> findByLikeSymbol( String symbol, Taxon taxon, TierType tier ) {
        return userRepository.findByGeneSymbolLike( symbol, taxon, tier );
    }

    @Override
    public Collection<User> findByLikeName( String nameLike ) {
        return userRepository.findByProfileNameContainingOrProfileLastNameContaining( nameLike, nameLike );
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

        return goService.convertTermTypes( terms, taxon, genes );
    }

    @Override
    public UserTerm convertTerms( User user, Taxon taxon, GeneOntologyTerm term ) {
        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return goService.convertTermTypes( term, taxon, genes );
    }

    @Override
    public Collection<UserTerm> recommendTerms( User user, Taxon taxon ) {
        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, TierType.MANUAL_TIERS );

        return goService.convertTermTypes( goService.recommendTerms( genes ), taxon, genes );
    }

    @Transactional
    @Override
    public void addGenesToUser( User user, Collection<UserGene> genes ) {
        int initialSize = user.getUserGenes().size();
//        user.getUserGenes().removeAll( genes );
        user.getUserGenes().addAll( genes );

        int added = user.getUserGenes().size() - initialSize;

        if ( added > 0 ) {
            log.info( "Added " + added + " genes to User " + user.getEmail() );
        }

        update( user );

    }

    @Transactional
    @Override
    public void removeGenesFromUser( User user, Collection<Gene> genes ) {

        int initialSize = user.getUserGenes().size();

        user.getUserGenes().removeIf( genes::contains );

        int removed = initialSize - user.getUserGenes().size();

        if ( removed > 0 ) {
            log.info( "Removed " + removed + " genes from User " + user.getEmail() );
        }

        update( user );
    }

    @Transactional
    @Override
    public void updateGenesInTaxon( User user, Taxon taxon, Collection<UserGene> genes ) {

        Collection<UserGene> newGenes = new HashSet<>( genes );

        newGenes.removeIf( g -> !g.getTaxon().equals( taxon ) );
        newGenes.addAll( calculatedGenesInTaxon( user, taxon ) );

        removeGenesFromUserByTaxon( user, taxon );
        addGenesToUser( user, newGenes );
    }

    @Transactional
    @Override
    public void updateGOTermsInTaxon( User user, Taxon taxon, Collection<GeneOntologyTerm> goTerms ) {
        removeTermsFromUserByTaxon( user, taxon );

        user.getUserTerms().addAll( convertTerms( user, taxon, goTerms ) );

        removeGenesFromUserByTiersAndTaxon( user, taxon, Collections.singleton( TierType.TIER3 ) );
        addGenesToUser( user, calculatedGenesInTaxon( user, taxon ) );
    }

    @Transactional
    @Override
    public void updateTermsAndGenesInTaxon( User user, Taxon taxon, Collection<UserGene> genes, Collection<GeneOntologyTerm> goTerms ) {
        Collection<UserGene> newGenes = genes.stream().filter( g -> g.getTaxon().equals( taxon ) ).collect( Collectors.toSet());

        removeTermsFromUserByTaxon( user, taxon );
        user.getUserTerms().addAll( convertTerms( user, taxon, goTerms ) );

        newGenes.addAll( calculatedGenesInTaxon( user, taxon ) );

        removeGenesFromUserByTaxon( user, taxon );
        addGenesToUser( user, newGenes );
    }

    @Transactional
    @Override
    public void updatePublications( User user, Set<Publication> publications ) {
        user.getProfile().getPublications().retainAll( publications );
        user.getProfile().getPublications().addAll( publications );
        update( user );
    }

    @Transactional
    @Override
    public void createPasswordResetTokenForUser( User user, String token ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        passwordResetTokenRepository.save( userToken );
    }

    @Override
    public void verifyPasswordResetToken( int userId, String token ) throws TokenException {
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken( token );
        if ( (passToken == null) || (passToken.getUser().getId() != userId) ) {
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
    public void createVerificationTokenForUser( User user, String token ) {
        VerificationToken userToken = new VerificationToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        tokenRepository.save( userToken );
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
        update( user );
        return user;
    }

    private Collection<UserGene> calculatedGenesInTaxon( User user, Taxon taxon ) {
        return goService.getRelatedGenes( user.getUserTerms(), taxon );
    }

    private boolean removeGenesFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserGenes().removeIf( ga -> ga.getTaxon().equals( taxon ) );
    }

    private boolean removeGenesFromUserByTiersAndTaxon( User user, Taxon taxon, Collection<TierType> tiers ) {
        return user.getUserGenes().removeIf( ga -> tiers.contains( ga.getTier() ) && ga.getTaxon().equals( taxon ) );
    }

    private boolean removeTermsFromUserByTaxon( User user, Taxon taxon ) {
        return user.getUserTerms().removeIf( ut -> ut.getTaxon().equals( taxon ) );
    }

}
