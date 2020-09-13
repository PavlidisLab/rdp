package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.settings.ApplicationSettings;

@CommonsLog
@Service
public class CacheService {

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private GeneInfoService geneInfoService;

    @Autowired
    private UserGeneService geneService;

    @Autowired
    private GOService goService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private UserOrganService userOrganService;

    /**
     * Cached data are updated periodically (monthly be default).
     *
     * This task make sure that the updates are performed in a logical order (i.e. genes are updated prior to ortholog
     * relationships and user-gene associations).
     */
    @Scheduled(fixedRate = 2592000000L)
    public void updateCache() {
        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();
        if ( cacheSettings.isEnabled() ) {
            log.info( "Updating cached data..." );
            geneInfoService.updateGenes();
            geneInfoService.updateGeneOrthologs();
            geneService.updateUserGenes();
            goService.updateGoTerms();
            userService.updateUserTerms();
            organInfoService.updateOrganInfos();
            userOrganService.updateUserOrgans();
            log.info("Done updating cached data. Next update is scheduled in 30 days from now.");
        }
    }
}
