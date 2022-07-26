package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
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
@CommonsLog
public class PurlResolver implements ProtocolResolver {

    private static final String PURL_PREFIX = "http://purl.obolibrary.org";

    @Override
    public Resource resolve( String location, ResourceLoader resourceLoader ) {
        if ( !location.startsWith( PURL_PREFIX ) ) {
            return null;
        }

        URL url;
        try {
            url = new URL( location );
        } catch ( MalformedURLException e ) {
            log.error( String.format( "Invalid PURL %s.", e ) );
            return null;
        }

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setInstanceFollowRedirects( false );
            if ( con.getHeaderField( "Location" ) != null ) {
                return new UrlResource( con.getHeaderField( "Location" ) );
            }
        } catch ( MalformedURLException e ) {
            log.warn( String.format( "Invalid 'Location' header for PURL %s.", url ) );
        } catch ( IOException e ) {
            log.error( String.format( "Failed to resolve PURL %s.", url ), e );
            if ( con != null ) {
                con.disconnect();
            }
        }
        return null;
    }
}
