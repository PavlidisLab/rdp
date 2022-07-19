package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.controllers.ApiController;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.net.URI;
import java.util.*;

/**
 * Interface for remote resource methods. These mirror methods from UserService and UserGeneService, but the implementation
 * fetches the results from remote origins.
 */
public interface RemoteResourceService {

    /**
     * Get the version of the remote API by reading its OpenAPI specification.
     *
     * @param remoteHost from which only the authority is used with {@link URI#getAuthority()}
     * @return the API version
     * @throws RemoteException if any error occured while retrieving the API version
     */
    String getApiVersion( URI remoteHost ) throws RemoteException;

    /**
     * Find users by name among all partner registries.
     *
     * @return matching users sorted according to {@link UserService#getUserComparator()}.
     * @see ApiController#searchUsersByName(String, Boolean, Set, Set, Set, String, String, Locale)
     */
    List<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos );

    /**
     * Find users by description among all partner registries.
     *
     * @return matching users sorted according to {@link UserService#getUserComparator()}.
     * @see ApiController#searchUsersByDescription(String, Set, Set, Set, String, String, Locale)
     */
    List<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos );

    List<User> findUsersByLikeNameAndDescription( String nameLike, boolean prefix, String descriptionLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos );

    /**
     * Find genes by symbol among all partner registries.
     *
     * @return matching genes sorted according to {@link UserGeneService#getUserGeneComparator()}.
     * @see ApiController#searchUsersByGeneSymbol(String, Integer, Set, Integer, Set, Set, Set, String, String, Locale)
     */
    List<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tier, Integer orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Set<String> organUberonIds, Collection<OntologyTermInfo> ontologyTermInfos );

    /**
     * Retrieve a user from a specific registry.
     *
     * @see ApiController#getUserById(Integer, String, String, Locale)
     */
    User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException;

    /**
     * Retrieve an anonymized user from a specific registry.
     *
     * @see ApiController#getUserByAnonymousId(UUID, String, String, Locale)
     */
    User getAnonymizedUser( UUID anonymousId, URI remoteHost ) throws RemoteException;
}
