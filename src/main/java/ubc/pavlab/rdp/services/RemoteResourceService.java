package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;

import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 * Interface for remote resource methods. These mirror methods from UserService and UserGeneService, but the implementation
 * fetches the results from remote origins.
 */
public interface RemoteResourceService {

    Collection<User> findUsersByLikeName( String nameLike, Boolean prefix, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds );

    Collection<User> findUsersByDescription( String descriptionLike, Collection<ResearcherCategory> researcherTypes, Collection<String> organUberonIds );

    Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tier, Integer orthologTaxonId, Set<ResearcherCategory> researcherTypes, Set<String> organUberonIds );

    User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException;
}
