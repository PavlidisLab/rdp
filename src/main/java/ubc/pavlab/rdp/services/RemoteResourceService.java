package ubc.pavlab.rdp.services;

import lombok.Builder;
import lombok.Value;
import net.bytebuddy.pool.TypePool;
import ubc.pavlab.rdp.controllers.ApiController;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.exception.UnknownRemoteApiException;
import ubc.pavlab.rdp.model.RemoteResource;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.RemoteOntologyTermInfo;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;

/**
 * Interface for remote resource methods. These mirror methods from UserService and UserGeneService, but the implementation
 * fetches the results from remote origins.
 * <p>
 * Some methods here return a {@link Future}. You might want to use {@link #collectFuture(URI, Future)} to extract the
 * value and convert potential exceptions into {@link RemoteException}.
 */
public interface RemoteResourceService {

    @Value
    @Builder
    class PartnerRegistry {
        /**
         * API URI used to query this registry.
         */
        URI apiUri;
        /**
         * Name of this partner registry.
         */
        String origin;
        /**
         * Indicate if access to this partner registry is authenticated with the search token from <code>rdp.settings.isearch.search-token</code>.
         */
        boolean authenticatedWithSearchToken;
    }

    /**
     * Collect a given future supplied by this service and convert any exception to an appropriate {@link RemoteException}
     *
     * @throws RemoteException      for any checked exception, note that {@link java.util.concurrent.ExecutionException}
     *                              are converted with their cause unwrapped as direct cause of the remote exception
     * @throws InterruptedException if the current thread was interrupted while waiting for the future to complete
     */
    <T> T collectFuture( URI uri, Future<T> future ) throws RemoteException, InterruptedException;

    /**
     * Obtain the list of URIs this registry is configured to communicate with.
     * <p>
     * Any authentication information is stripped.
     */
    List<URI> getApiUris();

    /**
     * Obtain the list of partner URIs this registry is configured to communicate with.
     */
    List<PartnerRegistry> getPartnerRegistries() throws RemoteException;

    /**
     * Get the version of the remote API by reading its OpenAPI specification.
     *
     * @param remoteHost from which only the authority is used with {@link URI#getAuthority()}
     * @return the API version
     * @throws RemoteException if any error occurred while retrieving the API version
     */
    String getApiVersion( URI remoteHost ) throws RemoteException;

    /**
     * Obtain n URL to view a remote user.
     */
    URI getViewUserUrl( User user ) throws UnknownRemoteApiException;

    /**
     * Obtain an API URI for a given remote host.
     */
    URI getRequestGeneAccessUrl( URI remoteHost, UUID anonymousId ) throws UnknownRemoteApiException;

    /**
     * Obtain a representative {@link RemoteResource} for a partner registry that can be used to extract the 'origin'
     * and 'originUrl' attributes.
     */
    Future<RemoteResource> getRepresentativeRemoteResource( URI remoteHost ) throws UnknownRemoteApiException;

    /**
     * Find users by name among all partner registries.
     *
     * @return matching users sorted according to {@link User#getComparator()}.
     * @see ApiController#searchUsersByName(String, Boolean, Set, Set, Set, List, List, Locale)
     */
    List<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos );

    /**
     * Find users by description among all partner registries.
     *
     * @return matching users sorted according to {@link User#getComparator()}.
     * @see ApiController#searchUsersByDescription(String, Set, Set, Set, List, List, Locale)
     */
    List<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos );

    List<User> findUsersByLikeNameAndDescription( String nameLike, boolean prefix, String descriptionLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos );

    /**
     * Find genes by symbol among all partner registries.
     *
     * @return matching genes sorted according to {@link UserGene#getComparator()}.
     * @see ApiController#searchUsersByGeneSymbol(String, Integer, Set, Integer, Set, Set, Set, List, List, Locale)
     */
    List<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tier, Integer orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherTypes, Set<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos );

    /**
     * Retrieve a user from a specific registry.
     *
     * @see ApiController#getUserById(Integer, Locale)
     */
    User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException;

    /**
     * Retrieve an anonymized user from a specific registry.
     *
     * @see ApiController#getUserByAnonymousId(UUID, Locale)
     */
    User getAnonymizedUser( UUID anonymousId, URI remoteHost ) throws RemoteException;

    /**
     * Find terms by ontology name and term IDs in a specific registry.
     *
     * @param ontology   ontology in which the search is performed
     * @param terms      terms, which must be part of the ontology
     * @param remoteHost partner API
     * @return a future containing the available terms, or containing null if the ontology is not present in the partner
     * registry
     * @see ApiController#getOntologyTermsByOntologyNameAndTermIds(String, List, Locale)
     */
    Future<List<RemoteOntologyTermInfo>> getTermsByOntologyNameAndTerms( Ontology ontology, Collection<OntologyTermInfo> terms, URI remoteHost ) throws RemoteException;
}
