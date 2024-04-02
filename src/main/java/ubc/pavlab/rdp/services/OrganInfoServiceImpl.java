package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.repositories.OrganInfoRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.OBOParser;
import ubc.pavlab.rdp.util.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@CommonsLog
@Service("organInfoService")
public class OrganInfoServiceImpl implements OrganInfoService {

    @Autowired
    private OrganInfoRepository organInfoRepository;

    @Autowired
    private OBOParser oboParser;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private ResourceLoader resourceLoader;

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

    @Override
    @Transactional
    public void updateOrganInfos() {
        Resource organFile;
        if ( applicationSettings.getCache().getOrganFile() != null && !applicationSettings.getCache().getOrganFile().isEmpty() ) {
            organFile = resourceLoader.getResource( applicationSettings.getCache().getOrganFile() );
        } else {
            log.warn( "No organ system ontology file found, skipping update." );
            return;
        }
        Collection<OBOParser.Term> parsedTerms;
        try ( Reader reader = new InputStreamReader( organFile.getInputStream() ) ) {
            parsedTerms = oboParser.parse( reader ).getTerms();
        } catch ( IOException | ParseException e ) {
            log.error( "Failed to load organ ontology.", e );
            return;
        }
        log.info( MessageFormat.format( "Loading organ ontology from {0}...", organFile ) );
        Map<String, OrganInfo> organInfosByUberonId = organInfoRepository.findAll().stream()
                .collect( Collectors.toMap( OrganInfo::getUberonId, identity() ) );
        for ( OBOParser.Term term : parsedTerms ) {
            OrganInfo organInfo = organInfosByUberonId.get( term.getId() );
            if ( organInfo == null ) {
                organInfo = new OrganInfo();
                organInfo.setUberonId( term.getId() );
                // only show organs that have been explicitly activated
                organInfo.setActive( false );
            }
            organInfo.setName( term.getName() );
            organInfo.setDescription( term.getDefinition() );
            organInfoRepository.save( organInfo );
        }
    }
}
