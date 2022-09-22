package ubc.pavlab.rdp.listeners;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.Assert;
import ubc.pavlab.rdp.events.OnOntologyUpdateEvent;
import ubc.pavlab.rdp.services.OntologyService;

/**
 * Listener for {@link ubc.pavlab.rdp.model.ontology.Ontology}-related events.
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class OntologyListener implements InitializingBean {

    @Autowired
    private CacheManager cacheManager;

    private Cache subtreeSizeCache;
    private Cache simpleOntologiesCache;

    @Override
    public void afterPropertiesSet() {
        subtreeSizeCache = cacheManager.getCache( OntologyService.SUBTREE_SIZE_BY_TERM_CACHE_NAME );
        Assert.notNull( subtreeSizeCache, String.format( "There is no cache named %s.", OntologyService.SIMPLE_ONTOLOGIES_CACHE_NAME ) );
        simpleOntologiesCache = cacheManager.getCache( OntologyService.SUBTREE_SIZE_BY_TERM_CACHE_NAME );
        Assert.notNull( simpleOntologiesCache, String.format( "There is no cache named %s.", OntologyService.SIMPLE_ONTOLOGIES_CACHE_NAME ) );
    }

    @TransactionalEventListener
    public void onOntologyUpdate( OnOntologyUpdateEvent event ) {
        log.info( String.format( "Evicting subtree sizes and simple ontologies caches after %s update.", event.getOntology() ) );
        // TODO: only evict terms in the given ontology
        subtreeSizeCache.invalidate();
        simpleOntologiesCache.invalidate();
    }
}
