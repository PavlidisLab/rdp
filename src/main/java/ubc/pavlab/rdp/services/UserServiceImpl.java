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
import ubc.pavlab.rdp.exception.PasswordResetException;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.UserGene.TierType;
import ubc.pavlab.rdp.repositories.PasswordResetTokenRepository;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.repositories.UserRepository;
import ubc.pavlab.rdp.util.GOTerm;

import javax.validation.ValidationException;
import java.util.*;

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
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private GOService goService;

    @Transactional
    @Override
    public void create( User user ) {
        user.setPassword( bCryptPasswordEncoder.encode( user.getPassword() ) );
        user.setActive( 1 ); //TODO activate here?
        Role userRole = roleRepository.findByRole( "USER" );

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

    @Transactional
    @Override
    public void addGenesToUser( User user, Map<Gene, TierType> genes ) {
        int initialSize = user.getGeneAssociations().size();

        for ( Map.Entry<Gene, TierType> entry : genes.entrySet() ) {
            Gene gene = entry.getKey();
            TierType tier = entry.getValue();

            UserGene ug = new UserGene();
            ug.setUser( user );
            ug.setTier( tier );
            ug.setGene( gene );

            user.getGeneAssociations().remove( ug );
            user.getGeneAssociations().add( ug );
        }

        int added = user.getGeneAssociations().size() - initialSize;

        if ( added > 0 ) {
            log.info( "Added " + added + " genes to User " + user.getEmail() );
        }

        update( user );

    }

    @Transactional
    @Override
    public void removeGenesFromUser( User user, Collection<Gene> genes ) {

        int initialSize = user.getGeneAssociations().size();

        for ( Gene gene : genes ) {
            UserGene ug = new UserGene();
            ug.setUser( user );
            ug.setGene( gene );

            user.getGeneAssociations().remove( ug );
        }

        int removed = initialSize - user.getGeneAssociations().size();

        if ( removed > 0 ) {
            log.info( "Removed " + removed + " genes from User " + user.getEmail() );
        }

        update( user );
    }

    @Transactional
    @Override
    public void updateGenesInTaxon( User user, Taxon taxon, Map<Gene, TierType> genes ) {

        Map<Gene, TierType> newGenes = new HashMap<>( genes );

        newGenes.entrySet().removeIf( entries -> !entries.getKey().getTaxon().equals( taxon ) );

        removeGenesFromUserByTaxon( user, taxon );

        for ( Gene g : calculatedGenesInTaxon( user, taxon ) ) {
            if ( !newGenes.containsKey( g ) ) {
                newGenes.put( g, TierType.TIER3 );
            }
        }

        addGenesToUser( user, newGenes );
    }

    @Transactional
    @Override
    public void updateGOTermsInTaxon( User user, Taxon taxon, Collection<GOTerm> goTerms ) {
        removeTermsFromUserByTaxon( user, taxon );

        Set<Gene> genes = user.getGenesByTaxonAndTier( taxon, new HashSet<>( Arrays.asList( TierType.TIER1, TierType.TIER2 ) ) );

        user.getGoTerms().addAll( goService.convertTermTypes( goTerms, taxon, genes ) );

        removeGenesFromUserByTiersAndTaxon( user, taxon, Collections.singleton( TierType.TIER3 ) );

        Map<Gene, TierType> genesToAdd = new HashMap<>();
        for ( Gene g : calculatedGenesInTaxon( user, taxon ) ) {
            if ( !genes.contains( g ) ) {
                genesToAdd.put( g, TierType.TIER3 );
            }
        }

        addGenesToUser( user, genesToAdd );
    }

    @Transactional
    @Override
    public void updatePublications( User user, Collection<Publication> publications ) {
        user.getProfile().getPublications().clear();
        user.getProfile().getPublications().addAll( publications );
        update( user );
    }

    @Transactional
    @Override
    public void createPasswordResetTokenForUser( User user, String token ) {
        PasswordResetToken userToken = new PasswordResetToken();
        userToken.setUser( user );
        userToken.updateToken( token );
        passwordResetTokenRepository.save(userToken);
    }

    @Override
    public void changePasswordByResetToken( int userId, String token, String newPassword ) throws PasswordResetException, ValidationException{
        PasswordResetToken passToken = passwordResetTokenRepository.findByToken(token);
        if ((passToken == null) || (passToken.getUser().getId() != userId)) {
            throw new PasswordResetException( "Invalid Token");
        }

        Calendar cal = Calendar.getInstance();
        if ((passToken.getExpiryDate()
                .getTime() - cal.getTime()
                .getTime()) <= 0) {
            throw new PasswordResetException( "Expired");
        }

        if ( newPassword.length() >= 6 ) { //TODO: Tie in with hibernate constraint on User or not necessary?

            // Preauthorize might cause trouble here if implemented, fix by setting manual authentication
            User user = findUserById( userId );

            user.setPassword( bCryptPasswordEncoder.encode( newPassword ) );
            update( user );
        } else {
            throw new ValidationException( "Password must be a minimum of 6 characters" );
        }
    }


    private Collection<Gene> calculatedGenesInTaxon( User user, Taxon taxon ) {
        return goService.getRelatedGenes( user.getGoTerms(), taxon );
    }

    private boolean removeGenesFromUserByTaxon( User user, Taxon taxon ) {
        return user.getGeneAssociations().removeIf( ga -> ga.getGene().getTaxon().equals( taxon ) );
    }

    private boolean removeGenesFromUserByTiersAndTaxon( User user, Taxon taxon, Collection<TierType> tiers ) {
        return user.getGeneAssociations().removeIf( ga -> tiers.contains( ga.getTier() ) && ga.getGene().getTaxon().equals( taxon ) );
    }

    private boolean removeTermsFromUserByTaxon( User user, Taxon taxon ) {
        return user.getGoTerms().removeIf( term -> term.getTaxon().equals( taxon ) );
    }

}
