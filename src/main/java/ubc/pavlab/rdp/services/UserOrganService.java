package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.UserOrgan;

import java.util.Collection;
import java.util.Set;

public interface UserOrganService {

    Collection<UserOrgan> findByDescription( String description );

    Collection<UserOrgan> findByUberonIdIn( Set<String> organUberonIds );
}
