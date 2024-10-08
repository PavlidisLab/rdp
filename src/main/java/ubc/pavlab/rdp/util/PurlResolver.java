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

import static ubc.pavlab.rdp.util.PurlUtils.isPurl;

/**
 * Resolve resources from <a href="http://purl.obolibrary.org">purl.obolibrary.org</a>.
 *
 * @author poirigui
 * @see PurlUtils
 */
@CommonsLog
public class PurlResolver implements ProtocolResolver {

    @Override
    public Resource resolve( String location, ResourceLoader resourceLoader ) {
        if ( !isPurl( location ) ) {
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
        } finally {
            if ( con != null ) {
                con.disconnect();
            }
        }
        return null;
    }
}
