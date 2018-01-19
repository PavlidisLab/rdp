package ubc.pavlab.rdp.services;

import org.springframework.security.access.annotation.Secured;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by mjacobson on 16/01/18.
 */
public interface UserService {

    @Secured( "ADMIN" )
    @Transactional
    void create( User user );

    @Transactional
    void update( User user );

    @Secured( "ADMIN" )
    @Transactional
    void delete( User user );

    String getCurrentUserName();

    String getCurrentEmail();

    User findCurrentUser();

    User findUserByEmail( String email );

    User findUserByUserName( String email );

    @Secured( "ADMIN" )
    List<User> findAll();

    Collection<User> findByGene( Gene gene );

    Collection<User> findByGene( Gene gene, UserGene.TierType tier );

    Collection<User> findByLikeSymbol( String symbol, Taxon taxon );

    Collection<User> findByLikeSymbol( String symbol, Taxon taxon, UserGene.TierType tier );

    Collection<User> findByLikeName( String nameLike );

    long countResearchers();

    Integer countResearchersWithGenes();

    @Transactional
    void addGenesToUser( User user, Map<Gene, UserGene.TierType> genes );

    @Transactional
    void removeGenesFromUser( User user, Collection<Gene> genes );

    @Transactional
    void updateGenesInTaxon( User user, Taxon taxon, Map<Gene, UserGene.TierType> genes );

    @Transactional
    void updateGOTermsInTaxon( User user, Collection<GeneOntologyTerm> goTerms, Taxon taxon );

    @Transactional
    void updatePublications( User user, Collection<Publication> publications );
}
