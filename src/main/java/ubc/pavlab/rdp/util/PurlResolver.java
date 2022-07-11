package ubc.pavlab.rdp.util;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;

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
}
