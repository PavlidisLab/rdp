package ubc.pavlab.rdp.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.UserOrgan;
import ubc.pavlab.rdp.repositories.UserOrganRepository;

import java.util.Collection;
import java.util.Set;

@Service("userOrganService")
public class UserOrganServiceImpl implements UserOrganService {

    @Autowired
    UserOrganRepository userOrganRepository;

    @Autowired
    PrivacyService privacyService;

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserOrgan> findByDescription( String description ) {
        return userOrganRepository.findByDescriptionContainingIgnoreCase( description );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserOrgan> findByUberonIdIn( Set<String> organUberonIds ) {
        return userOrganRepository.findByUberonIdIn(organUberonIds);
    }
}
