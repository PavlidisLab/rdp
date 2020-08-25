package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;

import java.util.Collection;

@Service("organInfoService")
public class OrganInfoServiceImpl implements OrganInfoService {

    @Autowired
    OrganInfoRepository organInfoRepository;

    @Override
    public Collection<OrganInfo> findAll() {
        return organInfoRepository.findAll();
    }

    @Override
    public Collection<OrganInfo> findByUberonIdIn( Collection<String> organUberonIds ) {
        return organInfoRepository.findByUberonIdIn(organUberonIds);
    }

    @Override
    public Collection<OrganInfo> findByActiveTrueOrderByOrdering() {
        return organInfoRepository.findByActiveTrueOrderByOrdering();
    }
}
