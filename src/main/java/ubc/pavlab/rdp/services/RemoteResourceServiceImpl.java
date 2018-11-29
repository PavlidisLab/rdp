package ubc.pavlab.rdp.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service("RemoteResourceService")
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";
    private static Log log = LogFactory.getLog( RemoteResourceServiceImpl.class );

    static {
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register( instance );
        instance.registerProvider( ResteasyJackson2Provider.class );
    }

    @Autowired
    private ApplicationSettings applicationSettings;

    @Override
    public Collection<User> findUsersByLikeName( String nameLike ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "nameLike", nameLike );
        }} );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "descriptionLike", descriptionLike );
        }} );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, TierType tier ) throws RemoteException {
        return convertRemoteGenes(
                getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, new HashMap<String, String>() {{
                    put( "symbol", symbol );
                    put( "taxonId", taxon.getId().toString() );
                    put( "tier", tier.toString() );
                }} ) );
    }

    @Override
    public Collection<UserGene> findGenesByLikeSymbol( String symbolLike, Taxon taxon, TierType tier )
            throws RemoteException {
        return convertRemoteGenes(
                getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, new HashMap<String, String>() {{
                    put( "symbolLike", symbolLike );
                    put( "taxonId", taxon.getId().toString() );
                    put( "tier", tier.toString() );
                }} ) );
    }

    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String uri, Map<String, String> args )
            throws RemoteException {
        Collection<T> entities = new HashSet<>();

        // Call all APIs
        for ( String api : applicationSettings.getIsearch().getApis() ) {
            @SuppressWarnings("unchecked") // Should be guaranteed when response is 200 and versions match.
                    Collection<T> received = remoteRequest( arrCls, api + uri, args );
            entities.addAll( received );
        }

        return entities;
    }

    private <T> Collection<T> remoteRequest( Class<T[]> arrCls, String remoteUrl, Map<String, String> args )
            throws RemoteException {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target( remoteUrl + encodeParams( args ) );
        Response response = target.request().get();
        if(response.getStatus() != 200){
            throw new RemoteException("No data received: "+response.readEntity( String.class ));
        }

        T[] arr = response.readEntity( arrCls );
        return Arrays.asList( arr );
    }

    private String encodeParams( Map<String, String> args ) {
        StringBuilder s = new StringBuilder( "?" );
        boolean first = true;
        for ( String arg : args.keySet() ) {
            if ( !first ) {
                s.append( "&" );
            } else {
                first = false;
            }
            try {
                s.append( arg ).append( "=" ).append( URLEncoder.encode( args.get( arg ), "UTF8" ) );
            } catch ( UnsupportedEncodingException e ) {
                log.error( e );
                e.printStackTrace();
            }
        }
        return s.toString();
    }

    private Collection<UserGene> convertRemoteGenes( Collection<UserGene> genes ) {
        for ( UserGene gene : genes ) {
            gene.setUser( gene.getRemoteUser() );
        }
        return genes;
    }

}
