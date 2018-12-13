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
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

@Service("RemoteResourceService")
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/%s";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";
    private static Log log = LogFactory.getLog( RemoteResourceServiceImpl.class );

    static {
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        RegisterBuiltin.register( instance );
        instance.registerProvider( ResteasyJackson2Provider.class );
    }

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Collection<User> findUsersByLikeName( String nameLike, Boolean prefix ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "nameLike", nameLike );
            put( "prefix", prefix.toString() );
        }} );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "descriptionLike", descriptionLike );
        }} );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, TierType tier, Integer homologueTaxonId )
            throws RemoteException {
        return convertRemoteGenes(
                getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, new HashMap<String, String>() {{
                    put( "symbol", symbol );
                    put( "taxonId", taxon.getId().toString() );
                    put( "tier", tier.toString() );
                    if ( homologueTaxonId != null ) {
                        put( "homologueTaxonId", homologueTaxonId.toString() );
                    }
                }} ) );
    }

    @Override
    public User getRemoteUser( Integer userId, String remoteHost ) throws RemoteException {
        // Check that the remoteHost is one of our known APIs and call it if it is.
        if ( Arrays.stream( applicationSettings.getIsearch().getApis() ).noneMatch( remoteHost::equals ) ) {
            return remoteRequest( User.class, remoteHost + String.format( API_USER_GET_URI, userId ),
                    addAuthParamIfAdmin( new HashMap<>() ) );
        }
        return null;
    }

    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String uri, Map<String, String> args )
            throws RemoteException {
        Collection<T> entities = new LinkedList<>();

        // Call all APIs
        for ( String api : applicationSettings.getIsearch().getApis() ) {
            @SuppressWarnings("unchecked") // Should be guaranteed when response is 200 and versions match.
                    Collection<T> received = Arrays
                    .asList( remoteRequest( arrCls, api + uri, addAuthParamIfAdmin( args ) ) );
            entities.addAll( received );
        }

        return entities;
    }

    private <T> T remoteRequest( Class<T> cls, String remoteUrl, Map<String, String> args ) throws RemoteException {
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target( remoteUrl + encodeParams( args ) );
        Response response = target.request().get();
        if ( response.getStatus() != 200 ) {
            log.error( "For request "+remoteUrl + encodeParams( args ) );
            throw new RemoteException( "No data received: " + response.readEntity( String.class ) );
        }

        return response.readEntity( cls );
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

    private Map<String, String> addAuthParamIfAdmin( Map<String, String> args ) {
        User user = userService.findCurrentUser();
        if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
            args.put( "auth", applicationSettings.getIsearch().getSearchToken() );
        }
        return args;
    }

}
