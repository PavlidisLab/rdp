package ubc.pavlab.rdp.services;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service("RemoteResourceService")
@CommonsLog
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/{userId}";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Collection<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", prefix.toString() );
        if ( researcherPositions != null ) {
            for ( ResearcherPosition researcherPosition : researcherPositions ) {
                params.add( "researcherPosition", researcherPosition.name() );
            }
        }
        if ( researcherCategories != null ) {
            for ( ResearcherCategory researcherCategory : researcherCategories ) {
                params.add( "researcherCategory", researcherCategory.name() );
            }
        }
        if ( organUberonIds != null ) {
            for ( String organUberonId : organUberonIds ) {
                params.add( "organUberonIds", organUberonId );
            }
        }
        addAuthParamIfAdmin( params );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "descriptionLike", descriptionLike );
        if ( researcherPositions != null ) {
            for ( ResearcherPosition researcherPosition : researcherPositions ) {
                params.add( "researcherPosition", researcherPosition.name() );
            }
        }
        if ( researcherCategories != null ) {
            for ( ResearcherCategory researcherCategory : researcherCategories ) {
                params.add( "researcherCategory", researcherCategory.name() );
            }
        }
        if ( organUberonIds != null ) {
            for ( String organUberonId : organUberonIds ) {
                params.add( "organUberonIds", organUberonId );
            }
        }
        addAuthParamIfAdmin( params );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds ) {
        List<UserGene> intlUsergenes = new LinkedList<>();
        for ( TierType tier : restrictTiers( tiers ) ) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add( "symbol", symbol );
            params.add( "taxonId", taxon.getId().toString() );
            // TODO: use the tiers field for the v1.4 API
            params.add( "tier", tier.toString() );
            if ( orthologTaxonId != null ) {
                params.add( "orthologTaxonId", orthologTaxonId.toString() );
            }
            if ( researcherPositions != null ) {
                for ( ResearcherPosition researcherPosition : researcherPositions ) {
                    params.add( "researcherPosition", researcherPosition.name() );
                }
            }
            if ( researcherCategories != null ) {
                for ( ResearcherCategory researcherCategory : researcherCategories ) {
                    params.add( "researcherCategory", researcherCategory.name() );
                }
            }
            if ( organUberonIds != null ) {
                for ( String organUberonId : organUberonIds ) {
                    params.add( "organUberonIds", organUberonId );
                }
            }
            addAuthParamIfAdmin( params );
            intlUsergenes.addAll( getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, params ) );
        }
        // add back-reference to user
        intlUsergenes.forEach( g -> g.setUser( g.getRemoteUser() ) );
        return intlUsergenes;
    }

    @Override
    public User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        String remoteHostAuthority = remoteHost.getAuthority();
        Map<String, URI> apiUriByAuthority = Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( URI::create )
                .collect( Collectors.toMap( URI::getAuthority, identity() ) );
        if ( !apiUriByAuthority.containsKey( remoteHost.getAuthority() ) ) {
            throw new RemoteException( MessageFormat.format( "Unknown remote API {0}.", remoteHost.getAuthority() ) );
        }

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        addAuthParamIfAdmin( queryParams );
        URI uri = UriComponentsBuilder.fromUri( apiUriByAuthority.get( remoteHostAuthority ) )
                .path( API_USER_GET_URI )
                .queryParams( queryParams )
                .buildAndExpand( Collections.singletonMap( "userId", userId ) )
                .toUri();

        User user = remoteRequest( User.class, uri );

        // add back-references to the user
        user.getUserGenes().values().forEach( ug -> ug.setUser( user ) );
        user.getUserTerms().forEach( ug -> ug.setUser( user ) );
        user.getUserOrgans().values().forEach( ug -> ug.setUser( user ) );

        return user;
    }

    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String path, MultiValueMap<String, String> params ) {
        Collection<T> entities = new LinkedList<>();

        // Call all APIs
        for ( String api : applicationSettings.getIsearch().getApis() ) {
            try {
                URI uri = UriComponentsBuilder.fromUriString( api )
                        .path( path )
                        .queryParams( params )
                        .build().toUri();
                entities.addAll( Arrays.asList( remoteRequest( arrCls, uri ) ) );
            } catch ( RemoteException e ) {
                log.error( MessageFormat.format( "Received error from remote API {0}:", api ), e );
            }
        }

        return entities;
    }

    private <T> T remoteRequest( Class<T> cls, URI uri ) throws RemoteException {
        ResponseEntity<T> responseEntity = restTemplate.getForEntity( uri, cls );

        if ( !responseEntity.getStatusCode().is2xxSuccessful() ) {
            throw new RemoteException( MessageFormat.format( "No data received from {0}.", uri ) );
        }

        return responseEntity.getBody();
    }

    private Set<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

    private void addAuthParamIfAdmin( MultiValueMap<String, String> query ) {
        User user = userService.findCurrentUser();
        if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
            query.add( "auth", applicationSettings.getIsearch().getSearchToken() );
        }
    }

}
