package ubc.pavlab.rdp.model;

import java.net.URI;

/**
 * Interface implemented by remote resources which can be retrieved with {@link ubc.pavlab.rdp.services.RemoteResourceService}.
 *
 * @author poirigui
 */
public interface RemoteResource {

    String getOrigin();

    void setOrigin( String origin );

    URI getOriginUrl();

    void setOriginUrl( URI originUrl );
}
