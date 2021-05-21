package ubc.pavlab.rdp.listeners;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ubc.pavlab.rdp.services.GOService;

@CommonsLog
@Component
public class GeneOntologyTermListener {

    @Autowired
    private GOService goService;

    @EventListener
    public void onApplicationStartup( ContextRefreshedEvent event ) {
        log.info( "Triggering update of GO terms..." );
        goService.updateGoTerms();
    }
}
