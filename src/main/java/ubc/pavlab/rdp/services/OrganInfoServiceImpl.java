package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;

import javax.transaction.Transactional;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;

@CommonsLog
@Service("organInfoService")
public class OrganInfoServiceImpl implements OrganInfoService {

    private static final String UBERON_URL = "http://purl.obolibrary.org/obo/uberon.obo";

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

    @Scheduled(fixedRate = 2592000000L)
    public void updateOrganInfos() {
        if ( !applicationSettings.getCache().isEnabled() ) {
            return;
        }
        try {
            InputStream in;
            String organFilePath = applicationSettings.getCache().getOrganFile();
            if ( applicationSettings.getCache().isLoadFromDisk() ) {
                in = new FileInputStream( organFilePath );
                log.info( MessageFormat.format( "Loading organ ontology from {0}.", organFilePath ) );
            } else {
                in = new URL( UBERON_URL ).openStream();
                log.info( MessageFormat.format( "Loading organ ontology from {0}.", UBERON_URL ) );
            }
            for ( OBOParser.Term term : oboParser.parseStream( in ).values() ) {
                OrganInfo organInfo = organInfoRepository.findByUberonId( term.getId() );
                if ( organInfo == null ) {
                    organInfo = new OrganInfo();
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
