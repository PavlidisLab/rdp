package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.Collection;

/**
 * Interface for remote resource methods. These mirror methods from UserService and UserGeneService, but the implementation
 * fetches the results from remote origins.
 */
public interface RemoteResourceService {
    Collection<User> findUsersByLikeName( String nameLike, Boolean prefix ) throws RemoteException;

    Collection<User> findUsersByDescription( String descriptionLike ) throws RemoteException;

    Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, TierType tier, Integer orthologTaxonId ) throws RemoteException;

    User getRemoteUser( Integer userId, String remoteHost ) throws RemoteException;
}
