package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.OrganInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrganInfoService {

    Collection<OrganInfo> findAll();

    Collection<OrganInfo> findByActiveTrueOrderByOrdering();

    Collection<OrganInfo> findByUberonIdIn( Collection<String> organUberonIds );
}
