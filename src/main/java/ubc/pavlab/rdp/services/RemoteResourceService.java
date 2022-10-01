package ubc.pavlab.rdp.services;

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
 */
public interface RemoteResourceService {

    /**
     * Obtain the list of URIs this registry is configured to communicate with.
     * <p>
     * Any authentication infomrmation is stripped.
     */
    List<URI> getApiUris();

    /**
     * Get the version of the remote API by reading its OpenAPI specification.
     *
     * @param remoteHost from which only the authority is used with {@link URI#getAuthority()}
     * @return the API version
     * @throws RemoteException if any error occured while retrieving the API version
     */
    String getApiVersion( URI remoteHost ) throws RemoteException;

    /**
     * Obtain a representative {@link RemoteResource} for a partner registry that can be used to extract the 'origin'
     * and 'originUrl' attributes.
     */
    RemoteResource getRepresentativeRemoteResource( URI remoteHost ) throws RemoteException;

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

    URI getApiUriByRemoteHost( URI remoteHost ) throws UnknownRemoteApiException;
}
