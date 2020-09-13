package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.UserOrgan;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;
import ubc.pavlab.rdp.repositories.UserOrganRepository;

import java.util.Collection;
import java.util.Set;

@CommonsLog
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

    @Autowired
    private OrganInfoRepository organInfoRepository;

    @Override
    @Transactional
    public void updateUserOrgans() {
        log.info( "Updating user organs..." );
        for ( UserOrgan userOrgan : userOrganRepository.findAll() ) {
            OrganInfo organInfo = organInfoRepository.findByUberonId( userOrgan.getUberonId() );
            if ( organInfo != null ) {
                userOrgan.updateOrgan( organInfo );
                userOrganRepository.save( userOrgan );
            }
        }
        log.info( "Done updating user organs." );
    }
}
