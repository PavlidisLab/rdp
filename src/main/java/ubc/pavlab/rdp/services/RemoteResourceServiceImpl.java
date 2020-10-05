package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service("RemoteResourceService")
@CommonsLog
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/%s";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ResteasyClient client;

    @Override
    public Collection<User> findUsersByLikeName( String nameLike, Boolean prefix, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<String>> organUberonIds ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new LinkedMultiValueMap<String, String>() {{
            add( "nameLike", nameLike );
            add( "prefix", prefix.toString() );
            for ( String organUberonId : organUberonIds.orElse( Collections.emptySet() ) ) {
                add( "organUberonIds", organUberonId );
            }
        }} );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<String>> organUberonIds ) throws RemoteException {
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, new LinkedMultiValueMap<String, String>() {{
            add( "descriptionLike", descriptionLike );
            for ( String organUberonId : organUberonIds.orElse( Collections.emptySet() ) ) {
                add( "organUberonIds", organUberonId );
            }
        }} );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer orthologTaxonId, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<String>> organUberonIds )
            throws RemoteException {
        List<UserGene> intlUsergenes = new LinkedList<>();
        // TODO: use the tiers field for the v1.4 API
        for ( TierType tier : restrictTiers( tiers ) ) {
            intlUsergenes.addAll(
                    getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, new LinkedMultiValueMap<String, String>() {{
                        add( "symbol", symbol );
                        add( "taxonId", taxon.getId().toString() );
                        add( "tier", tier.toString() );
                        if ( orthologTaxonId != null ) {
                            add( "orthologTaxonId", orthologTaxonId.toString() );
                        }
                        for ( String organUberonId : organUberonIds.orElse( Collections.emptySet() ) ) {
                            add( "organUberonIds", organUberonId );
                        }
                    }} ) );
        }
        // add back-reference to user
        intlUsergenes.forEach( g -> g.setUser( g.getRemoteUser() ) );
        return intlUsergenes;
    }

    @Override
    public User getRemoteUser( Integer userId, String remoteHost ) throws RemoteException {
        // Check that the remoteHost is one of our known APIs and call it if it is.
        if ( Arrays.stream( applicationSettings.getIsearch().getApis() ).anyMatch( remoteHost::equals ) ) {
            User user = remoteRequest( User.class, remoteHost + String.format( API_USER_GET_URI, userId ),
                    addAuthParamIfAdmin( new LinkedMultiValueMap<>() ) );
            // add back-references to the user
            user.getUserGenes().values().forEach( ug -> ug.setUser( user ) );
            user.getUserTerms().forEach( ug -> ug.setUser( user ) );
            user.getUserOrgans().values().forEach( ug -> ug.setUser( user ) );
            return user;
        } else {
            throw new RemoteException( MessageFormat.format( "Unknown remote API {0}.", remoteHost ) );
        }
    }

    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String uri, MultiValueMap<String, String> args )
            throws RemoteException {
        Collection<T> entities = new LinkedList<>();

        // Call all APIs
        for ( String api : applicationSettings.getIsearch().getApis() ) {
            try {
                entities.addAll( Arrays.asList( remoteRequest( arrCls, api + uri, addAuthParamIfAdmin( args ) ) ) );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Received error from remote API {0}:", api ), e );
            }
        }

        return entities;
    }

    private <T> T remoteRequest( Class<T> cls, String remoteUrl, MultiValueMap<String, String> args ) throws RemoteException {
        ResteasyWebTarget target = client.target( remoteUrl + encodeParams( args ) );
        Response response = target.request().get();
        if ( response.getStatus() != 200 ) {
            throw new RemoteException( "No data received: " + response.readEntity( String.class ) );
        }

        return response.readEntity( cls );
    }

    private String encodeParams( MultiValueMap<String, String> args ) {
        StringBuilder s = new StringBuilder( "?" );
        boolean first = true;
        for ( String arg : args.keySet() ) {
            if ( !first ) {
                s.append( "&" );
            } else {
                first = false;
            }
            try {
                for ( String a : args.get( arg ) ) {
                    s.append( arg ).append( "=" ).append( URLEncoder.encode( a, "UTF8" ) );
                }
            } catch ( UnsupportedEncodingException e ) {
                log.error( e );
                e.printStackTrace();
            }
        }
        return s.toString();
    }

    private Set<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

    private MultiValueMap<String, String> addAuthParamIfAdmin( MultiValueMap<String, String> args ) {
        User user = userService.findCurrentUser();
        if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
            args.add( "auth", applicationSettings.getIsearch().getSearchToken() );
        }
        return args;
    }

}
