package ubc.pavlab.rdp.services;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Builder;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.util.UriComponents;
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
import ubc.pavlab.rdp.util.VersionUtils;

import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service("RemoteResourceService")
@CommonsLog
@PreAuthorize("hasPermission(null, 'international-search')")
public class RemoteResourceServiceImpl implements RemoteResourceService {

    private static final String API_URI = "/api";
    private static final String API_USERS_SEARCH_URI = "/api/users/search";
    private static final String API_USER_GET_URI = "/api/users/{userId}";
    private static final String API_USER_GET_BY_ANONYMOUS_ID_URI = "/api/users/by-anonymous-id/{anonymousId}";
    private static final String API_GENES_SEARCH_URI = "/api/genes/search";

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    @Autowired
    private TaxonService taxonService;

    @Override
    @Cacheable(value = "ubc.pavlab.rdp.services.RemoteResourceService.apiVersionByRemoteHostAuthority", key = "#remoteHost.authority")
    public String getApiVersion( URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI authority = getApiUri( remoteHost );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_URI )
                .build()
                .toUri();
        try {
            OpenAPI openAPI = asyncRestTemplate.getForEntity( uri, OpenAPI.class ).get().getBody();
            // The OpenAPI specification was introduced in 1.4, so we assume 1.0.0 for previous versions
            if ( openAPI.getInfo() == null ) {
                return "1.0.0";
            } else if ( openAPI.getInfo().getVersion().equals( "v0" ) ) {
                return "1.4.0"; // the version number was missing in early 1.4
            } else {
                return openAPI.getInfo().getVersion();
            }
        } catch ( InterruptedException | ExecutionException e ) {
            throw new RemoteException( MessageFormat.format( "Unsuccessful response received for {0}.", uri ), e );
        }
    }

    @Override
    public Collection<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", prefix.toString() );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .build().toMultiValueMap() );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params );
    }

    @Override
    public Collection<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "descriptionLike", descriptionLike );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .build().toMultiValueMap() );
        return getRemoteEntities( User[].class, API_USERS_SEARCH_URI, params );
    }

    @Override
    public Collection<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds ) {
        List<UserGene> intlUsergenes = new LinkedList<>();
        for ( TierType tier : restrictTiers( tiers ) ) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add( "symbol", symbol );
            params.putAll( UserGeneSearchParams.builder()
                    .taxonId( taxon.getId() )
                    .tier( tier )
                    .orthologTaxonId( orthologTaxonId )
                    .researcherPositions( researcherPositions )
                    .researcherCategories( researcherCategories )
                    .organUberonIds( organUberonIds )
                    .build().toMultiValueMap() );
            intlUsergenes.addAll( getRemoteEntities( UserGene[].class, API_GENES_SEARCH_URI, params ) );
        }
        Map<Integer, Integer> taxonOrderingById = taxonService.findByActiveTrue().stream()
                .collect( Collectors.toMap( Taxon::getId, Taxon::getOrdering ) );
        for ( UserGene g : intlUsergenes ) {
            // add back-reference to user
            g.setUser( g.getRemoteUser() );
            // populate taxon ordering
            g.getTaxon().setOrdering( taxonOrderingById.getOrDefault( g.getTaxon().getId(), null ) );
        }
        return intlUsergenes;
    }

    @Override
    public User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI authority = getApiUri( remoteHost );

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        addAuthParamIfAdmin( authority, queryParams );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_USER_GET_URI )
                .replaceQueryParams( queryParams )
                .buildAndExpand( Collections.singletonMap( "userId", userId ) )
                .toUri();

        return getUserByUri( uri );
    }

    @Override
    public User getAnonymizedUser( UUID anonymousId, URI remoteHost ) throws RemoteException {
        URI authority = getApiUri( remoteHost );

        if ( !VersionUtils.satisfiesVersion( getApiVersion( remoteHost ), "1.4.0" ) ) {
            log.info( MessageFormat.format( "{0} does not support retrieving user by anonymous identifier, will return null instead.", remoteHost ) );
            return null;
        }

        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        addAuthParamIfAdmin( authority, queryParams );
        URI uri = UriComponentsBuilder.fromUri( authority )
                .path( API_USER_GET_BY_ANONYMOUS_ID_URI )
                .replaceQueryParams( queryParams )
                .buildAndExpand( Collections.singletonMap( "anonymousId", anonymousId ) )
                .toUri();

        return getUserByUri( uri );
    }

    private User getUserByUri( URI uri ) throws RemoteException {
        try {
            ResponseEntity<User> responseEntity = asyncRestTemplate.getForEntity( uri, User.class ).get();
            User user = responseEntity.getBody();
            initUser( user );
            return user;
        } catch ( InterruptedException | ExecutionException e ) {
            throw new RemoteException( MessageFormat.format( "Unsuccessful response received for {0}.", uri ), e );
        }
    }

    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String path, MultiValueMap<String, String> params ) {
        return Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( URI::create )
                .map( api -> {
                    // work on a copy because we'll be selectively adding auth information
                    LinkedMultiValueMap<String, String> apiParams = new LinkedMultiValueMap<>( params );
                    addAuthParamIfAdmin( api, apiParams );
                    return UriComponentsBuilder.fromUri( api )
                            .path( path )
                            .replaceQueryParams( apiParams )
                            .build().toUri();
                } )
                .map( uri -> Pair.of( uri, asyncRestTemplate.getForEntity( uri, arrCls ) ) )
                // it's important to collect, otherwise the future will be created and joined on-by-one, defeating the purpose of using them
                .collect( Collectors.toList() ).stream()
                .map( uriAndFuture -> {
                    try {
                        return uriAndFuture.getRight().get( applicationSettings.getIsearch().getRequestTimeout(), TimeUnit.SECONDS );
                    } catch ( InterruptedException | ExecutionException | TimeoutException e ) {
                        log.error( MessageFormat.format( "Unsuccessful response received for {0}.", uriAndFuture.getLeft() ), e );
                        return null;
                    }
                } )
                .filter( Objects::nonNull )
                .map( ResponseEntity::getBody )
                .flatMap( Arrays::stream )
                .collect( Collectors.toList() );
    }

    private URI getApiUri( URI remoteHost ) throws RemoteException {
        String remoteHostAuthority = remoteHost.getAuthority();
        Map<String, URI> apiUriByAuthority = Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( URI::create )
                .collect( Collectors.toMap( URI::getAuthority, identity() ) );
        if ( !apiUriByAuthority.containsKey( remoteHost.getAuthority() ) ) {
            throw new RemoteException( MessageFormat.format( "Unknown remote API {0}.", remoteHost.getAuthority() ) );
        }
        return apiUriByAuthority.get( remoteHostAuthority );
    }

    private void initUser( User user ) {
        user.getUserGenes().values().forEach( ug -> ug.setUser( user ) );
        user.getUserTerms().forEach( ug -> ug.setUser( user ) );
        user.getUserOrgans().values().forEach( ug -> ug.setUser( user ) );
    }

    private Set<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toSet() );
    }

    private void addAuthParamIfAdmin( URI apiUri, MultiValueMap<String, String> query ) {
        User user = userService.findCurrentUser();
        if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
            UriComponents apiUriComponents = UriComponentsBuilder.fromUri( apiUri ).build();
            if ( apiUriComponents.getQueryParams().containsKey( "noauth" ) ) {
                // do nothing, we don't have admin access for this partner
            } else if ( apiUriComponents.getQueryParams().containsKey( "auth" ) ) {
                // use a specific search token
                query.add( "auth", apiUriComponents.getQueryParams().getFirst( "auth" ) );
            } else {
                // use the default search token
                query.add( "auth", applicationSettings.getIsearch().getSearchToken() );
            }
        }
    }

    @Data
    @Builder
    static class UserSearchParams {

        private Collection<ResearcherPosition> researcherPositions;
        private Collection<ResearcherCategory> researcherCategories;
        private Collection<String> organUberonIds;

        public MultiValueMap<String, String> toMultiValueMap() {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
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
            return params;
        }
    }

    @Data
    @Builder
    static class UserGeneSearchParams {

        private Integer taxonId;
        private TierType tier;
        private Integer orthologTaxonId;
        private Collection<ResearcherPosition> researcherPositions;
        private Collection<ResearcherCategory> researcherCategories;
        private Collection<String> organUberonIds;

        public MultiValueMap<String, String> toMultiValueMap() {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add( "taxonId", taxonId.toString() );
            params.add( "tier", tier.name() );
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
            return params;
        }

    }

}
