package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
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
@CommonsLog
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/%s";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";

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
    public Collection<User> findUsersByLikeName( String nameLike, Boolean prefix, Optional<Collection<String>> organUberonIds ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "nameLike", nameLike );
            put( "prefix", prefix.toString() );
        }} );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike, Optional<Collection<String>> organUberonIds ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new HashMap<String, String>() {{
            put( "descriptionLike", descriptionLike );
        }} );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer orthologTaxonId, Optional<Collection<String>> organUberonIds )
            throws RemoteException {
        List<UserGene> intlUsergenes = new LinkedList<>();
        // TODO: use the tiers field for the v1.4 API
        for ( TierType tier : tiers ) {
            intlUsergenes.addAll( convertRemoteGenes(
                    getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, new HashMap<String, String>() {{
                        put( "symbol", symbol );
                        put( "taxonId", taxon.getId().toString() );
                        put( "tier", tier.toString() );
                        if ( orthologTaxonId != null ) {
                            put( "orthologTaxonId", orthologTaxonId.toString() );
                        }
                    }} ) ) );
        }
        return intlUsergenes;
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
        List<RemoteException> errs = new LinkedList<>();

        // Call all APIs
        for ( String api : applicationSettings.getIsearch().getApis() ) {
            try {
                @SuppressWarnings("unchecked") // Should be guaranteed when response is 200 and versions match.
                        Collection<T> received = Arrays
                        .asList( remoteRequest( arrCls, api + uri, addAuthParamIfAdmin( args ) ) );
                entities.addAll( received );
            } catch ( RemoteException e ) {
                errs.add( e );
            }
        }

        if ( !errs.isEmpty() ) {
            if(entities.isEmpty()) {
                // Only throw when there are no results to be displayed
                throw errs.get( 0 );
            }
            // Always log errors
            for(RemoteException e : errs) {
                log.error(e);
            }
        }

        return entities;
    }

    private <T> T remoteRequest( Class<T> cls, String remoteUrl, Map<String, String> args ) throws RemoteException {

        String proxyHost = applicationSettings.getIsearch().getHost();
        String proxyPort = applicationSettings.getIsearch().getPort();
        ResteasyClient client = null;

        if (proxyHost != null && proxyPort != null &&
                !proxyHost.equals("") && !proxyPort.equals("") ) {
            client = new ResteasyClientBuilder().defaultProxy(
                    proxyHost,
                    Integer.parseInt( proxyPort )
            ).build();
            log.info( "Using " +proxyHost + ":" + proxyPort+ " as proxy for rest client." );
        } else {
            client = new ResteasyClientBuilder().build();
            log.info( "Using default proxy for rest client." );
        }

        ResteasyWebTarget target = client.target( remoteUrl + encodeParams( args ) );
        Response response = target.request().get();
        if ( response.getStatus() != 200 ) {
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
