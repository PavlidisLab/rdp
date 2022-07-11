package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolve resources from <a href="http://purl.obolibrary.org">purl.obolibrary.org</a>.
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class PurlResolver implements ProtocolResolver, ResourceLoaderAware, ApplicationContextAware {

    /**
     * Check if the provided URL refers to a PURL resource.
     */
    private static boolean isPurl( URL url ) {
        return url.getProtocol().equals( "http" ) && url.getHost().equals( "purl.obolibrary.org" );
    }

    @Override
    public Resource resolve( String location, ResourceLoader resourceLoader ) {
        HttpURLConnection con = null;
        try {
            URL url = new URL( location );
            if ( isPurl( url ) ) {
                con = (HttpURLConnection) url.openConnection();
                con.setInstanceFollowRedirects( false );
                if ( con.getHeaderField( "Location" ) != null ) {
                    return new UrlResource( con.getHeaderField( "Location" ) );
                }
            }
        } catch ( MalformedURLException e ) {
            return null;
        } catch ( IOException e ) {
            log.error( "Failed to resolve %s.", e );
            if ( con != null ) {
                con.disconnect();
            }
        }
        return null;
    }

    @Override
    public void setResourceLoader( ResourceLoader resourceLoader ) {
        if ( resourceLoader instanceof DefaultResourceLoader ) {
            ( (DefaultResourceLoader) resourceLoader ).addProtocolResolver( new PurlResolver() );
            log.info( String.format( "Registered %s to the resource loader %s.", PurlResolver.class, resourceLoader ) );
        } else {
            log.warn( String.format( "Could not register %s to the resource loader %s. PURL URLs might not be resolved correctly.", PurlResolver.class, resourceLoader ) );
        }
    }

    @Override
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        // FIXME: This is necessary because protocol resolvers are not honored if a resource loader is set on the application context (see https://github.com/spring-projects/spring-framework/issues/28703)
        if ( applicationContext instanceof GenericApplicationContext ) {
            GenericApplicationContext genericApplicationContext = (GenericApplicationContext) applicationContext;
            genericApplicationContext.setResourceLoader( null );
        }
    }
}
