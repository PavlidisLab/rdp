package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;

@CommonsLog
@Service("organInfoService")
public class OrganInfoServiceImpl implements OrganInfoService {

    @Autowired
    OrganInfoRepository organInfoRepository;

    @Autowired
    private OBOParser oboParser;

    @Override
    public Collection<OrganInfo> findAll() {
        return organInfoRepository.findAll();
    }

    @Override
    public Collection<OrganInfo> findByUberonIdIn( Collection<String> organUberonIds ) {
        return organInfoRepository.findByUberonIdIn( organUberonIds );
    }

    @Override
    public Collection<OrganInfo> findByActiveTrueOrderByOrdering() {
        return organInfoRepository.findByActiveTrueOrderByOrdering();
    }

    @Autowired
    ApplicationSettings applicationSettings;

    @Override
    public void updateOrganInfos() {
        try {
            Resource organFile = applicationSettings.getCache().getOrganFile();
            log.info( MessageFormat.format( "Loading organ ontology from {0}...", organFile ) );
            for ( OBOParser.Term term : oboParser.parseStream( organFile.getInputStream() ).values() ) {
                OrganInfo organInfo = organInfoRepository.findByUberonId( term.getId() );
                if ( organInfo == null ) {
                    organInfo = new OrganInfo();
	            organInfo.setName( term.getName() );
                    organInfo.setUberonId( term.getId() );
                    // only show organs that have been explicitly activated
                    organInfo.setActive( false );
                }
                
                organInfo.setDescription( term.getDefinition() );
                organInfoRepository.save( organInfo );
            }
        } catch ( IOException e ) {
            log.error( "Failed to load organ ontology.", e );
        }

    }
}
