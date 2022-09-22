package ubc.pavlab.rdp;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import ubc.pavlab.rdp.util.PurlResolver;

@CommonsLog
@Configuration
public class ResourceLoaderConfig implements ResourceLoaderAware {

    @Override
    public void setResourceLoader( ResourceLoader resourceLoader ) {
        if ( resourceLoader instanceof DefaultResourceLoader ) {
            ( (DefaultResourceLoader) resourceLoader ).addProtocolResolver( new PurlResolver() );
            log.info( String.format( "Registered %s to the resource loader %s.", PurlResolver.class, resourceLoader ) );
        } else {
            log.warn( String.format( "Could not register %s to the resource loader %s. PURL URLs might not be resolved correctly.", PurlResolver.class, resourceLoader ) );
        }
    }
}
