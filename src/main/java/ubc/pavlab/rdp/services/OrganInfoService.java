package ubc.pavlab.rdp.services;

import org.springframework.scheduling.annotation.Scheduled;
import ubc.pavlab.rdp.model.OrganInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface OrganInfoService {

    Collection<OrganInfo> findAll();

    Collection<OrganInfo> findByActiveTrueOrderByOrdering();

    Collection<OrganInfo> findByUberonIdIn( Collection<String> organUberonIds );

    void updateOrganInfos();
}
