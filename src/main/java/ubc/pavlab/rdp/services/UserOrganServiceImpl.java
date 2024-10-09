package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.UserOrgan;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;
import ubc.pavlab.rdp.repositories.UserOrganRepository;

@CommonsLog
@Service("userOrganService")
public class UserOrganServiceImpl implements UserOrganService {

    @Autowired
    private UserOrganRepository userOrganRepository;

    @Autowired
    private OrganInfoRepository organInfoRepository;

    @Override
    @Transactional
    public void updateUserOrgans() {
        log.info( "Updating user organs..." );
        for ( UserOrgan userOrgan : userOrganRepository.findAll() ) {
            organInfoRepository.findByUberonId( userOrgan.getUberonId() ).ifPresent( organInfo -> {
                userOrgan.updateOrgan( organInfo );
                userOrganRepository.save( userOrgan );
            } );
        }
        log.info( "Done updating user organs." );
    }
}
