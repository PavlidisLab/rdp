package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Organ;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.repositories.UserOrganRepository;

import java.util.Collection;
import java.util.stream.Collectors;

@Service("organService")
public class UserOrganServiceImpl implements UserOrganService {

    @Autowired
    UserOrganRepository userOrganRepository;

    @Autowired
    PrivacyService privacyService;

    @Override
    @PostFilter("hasPermission(filteredObject, 'read')")
    public Collection<Organ> findByDescriptionAndTaxon( String description, Taxon taxon ) {
        return userOrganRepository.findByDescriptionContainingIgnoreCaseAndTaxon( description, taxon )
                .stream()
                .collect( Collectors.toList() );
    }
}
