package ubc.pavlab.rdp.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.exception.UnknownRemoteApiException;
import ubc.pavlab.rdp.model.RemoteResource;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.RemoteOntologyTermInfo;
import ubc.pavlab.rdp.repositories.RoleRepository;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.VersionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@Service("remoteResourceService")
@CommonsLog
@PreAuthorize("hasPermission(null, 'international-search')")
public class RemoteResourceServiceImpl implements RemoteResourceService, InitializingBean {

    private static final String API_ROOT_PATH = "/api";
    private static final String GET_OPENAPI_PATH = API_ROOT_PATH;
    private static final String SEARCH_USERS_PATH = API_ROOT_PATH + "/users/search";
    private static final String GET_USER_PATH = API_ROOT_PATH + "/users/{userId}";
    private static final String GET_USER_BY_ANONYMOUS_ID_PATH = API_ROOT_PATH + "/users/by-anonymous-id/{anonymousId}";
    private static final String SEARCH_GENES_PATH = API_ROOT_PATH + "/genes/search";
    private static final String GET_ONTOLOGY_TERMS_PATH = API_ROOT_PATH + "/ontologies/{ontologyName}/terms";

    @Autowired
    private ApplicationSettings applicationSettings;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    @Qualifier("remoteResourceRestTemplate")
    private AsyncRestTemplate asyncRestTemplate;

    @Autowired
    private TaxonService taxonService;

    /**
     * For properly self-referencing when calling {@link #getApiVersion(URI)}.
     */
    @Lazy
    @Autowired
    private RemoteResourceService remoteResourceService;

    @Override
    @SneakyThrows(URISyntaxException.class)
    public void afterPropertiesSet() {
        for ( URI apiUri : applicationSettings.getIsearch().getApis() ) {
            URI uri = UriComponentsBuilder.fromUri( apiUri )
                    .path( API_ROOT_PATH )
                    .build().toUri();
            URI expectedOriginUrl = new URI( apiUri.getScheme(), apiUri.getAuthority(), StringUtils.trimTrailingCharacter( apiUri.getPath(), '/' ), null, null );
            asyncRestTemplate.getForEntity( uri, OpenAPI.class ).completable()
                    .thenApply( re -> extractRemoteResource( Objects.requireNonNull( re.getBody(), String.format( "Unexpected null response body for %s.", uri ) ), apiUri ) )
                    .handle( ( rr, ex ) -> {
                        if ( ex == null ) {
                            if ( !rr.getOriginUrl().equals( expectedOriginUrl ) ) {
                                log.warn( String.format( "Partner registry %s's origin URL %s differs from API URL in the configuration: %s.", rr.getOrigin(), rr.getOriginUrl(), apiUri ) );
                            }
                        } else {
                            log.warn( String.format( "Failed to reach partner registry %s: %s", expectedOriginUrl, ExceptionUtils.getRootCauseMessage( ex ) ) );
                        }
                        return null;
                    } );
        }
    }

    @Override
    @Cacheable(value = "ubc.pavlab.rdp.services.RemoteResourceService.apiVersionByRemoteHostAuthority", key = "#remoteHost.authority")
    public String getApiVersion( URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI apiUri = getApiUri( remoteHost, false );
        URI uri = UriComponentsBuilder.fromUri( apiUri )
                .path( GET_OPENAPI_PATH )
                .build()
                .toUri();
        OpenAPI openAPI = getFromRequestFuture( uri, asyncRestTemplate.getForEntity( uri, OpenAPI.class ) );
        // The OpenAPI specification was introduced in 1.4, so we assume 1.0.0 for previous versions
        if ( openAPI.getInfo() == null ) {
            return "1.0.0";
        } else if ( openAPI.getInfo().getVersion().equals( "v0" ) ) {
            return "1.4.0"; // the version number was missing in early 1.4
        } else {
            return openAPI.getInfo().getVersion();
        }
    }

    @Override
    public URI getViewUserUrl( User user ) throws UnknownRemoteApiException {
        if ( user.getId() == null ) {
            throw new IllegalArgumentException( "User must have a non-null ID." );
        }
        if ( user.getOriginUrl() == null ) {
            throw new IllegalArgumentException( "User must be a remote user with an origin URL." );
        }
        return UriComponentsBuilder.fromUri( getApiUri( user.getOriginUrl(), false ) )
                .path( "/userView/{userId}" )
                .build( user.getId() );
    }

    @Override
    public URI getRequestGeneAccessUrl( URI remoteHost, UUID anonymousId ) throws UnknownRemoteApiException {
        return UriComponentsBuilder.fromUri( getApiUri( remoteHost, false ) )
                .path( "/search/gene/by-anonymous-id/{anonymousId}/request-access" )
                .build( anonymousId );
    }

    @Override
    public RemoteResource getRepresentativeRemoteResource( URI remoteHost ) throws RemoteException {
        URI uri = UriComponentsBuilder.fromUri( getApiUri( remoteHost, false ) )
                .path( API_ROOT_PATH )
                .build().toUri();
        return extractRemoteResource( getFromRequestFuture( uri, asyncRestTemplate.getForEntity( uri, OpenAPI.class ) ), remoteHost );
    }

    private static RemoteResource extractRemoteResource( OpenAPI openAPI, URI remoteHost ) {
        Pattern titlePattern = Pattern.compile( "^(.+) RESTful API$" );
        String origin = remoteHost.getAuthority();
        URI originUrl = remoteHost;
        if ( openAPI.info != null && openAPI.info.title != null ) {
            Matcher matcher = titlePattern.matcher( openAPI.info.title );
            if ( matcher.matches() ) {
                origin = matcher.group( 1 );
            }
        }
        if ( openAPI.servers != null && openAPI.servers.size() == 1 ) {
            originUrl = openAPI.servers.get( 0 ).url;
        }
        return new SimpleRemoteResource( origin, originUrl );
    }

    @Data
    @AllArgsConstructor
    private static class SimpleRemoteResource implements RemoteResource {
        private String origin;
        private URI originUrl;
    }

    @Data
    private static class OpenAPI {

        @Data
        public static class Info {
            private String title;
            private String version;
        }

        @Data
        public static class Server {
            private URI url;
        }

        private Info info;
        private List<Server> servers;
    }

    @Override
    public List<User> findUsersByLikeName( String nameLike, Boolean prefix, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", prefix.toString() );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        RequestFilter<User[]> filters;
        if ( ontologyTermInfos != null ) {
            filters = satisfiesVersion( "1.5.0" );
        } else {
            filters = satisfiesVersion( "1.0.0" );
        }
        return getRemoteEntities( User[].class, SEARCH_USERS_PATH, params, filters ).stream()
                .map( this::initUser )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<User> findUsersByDescription( String descriptionLike, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherCategories, Collection<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "descriptionLike", descriptionLike );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        RequestFilter<User[]> filters;
        if ( ontologyTermInfos != null ) {
            filters = satisfiesVersion( "1.5.0" );
        } else {
            filters = satisfiesVersion( "1.0.0" );
        }
        return getRemoteEntities( User[].class, SEARCH_USERS_PATH, params, filters ).stream()
                .map( this::initUser )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }

    @Override
    public List<User> findUsersByLikeNameAndDescription( String nameLike, boolean prefix, String descriptionLike, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add( "nameLike", nameLike );
        params.add( "prefix", String.valueOf( prefix ) );
        params.add( "descriptionLike", descriptionLike );
        params.putAll( UserSearchParams.builder()
                .researcherPositions( researcherPositions )
                .researcherCategories( researcherCategories )
                .organUberonIds( organUberonIds )
                .ontologyTermInfos( ontologyTermInfos )
                .build().toMultiValueMap() );
        RequestFilter<User[]> requestFilters = ( apiUri, chain ) -> {
            String apiVersion;
            try {
                apiVersion = remoteResourceService.getApiVersion( apiUri );
            } catch ( RemoteException e ) {
                log.warn( String.format( "Failed to retrieve API version from %s: %s.", apiUri, ExceptionUtils.getRootCauseMessage( e ) ) );
                return null;
            }
            if ( VersionUtils.satisfiesVersion( apiVersion, "1.5.0" ) ) {
                return chain.filter( apiUri, chain );
            } else if ( ontologyTermInfos != null ) {
                // pre-1.5 registry do not support ontology terms
                return CompletableFuture.completedFuture( ResponseEntity.ok( new User[0] ) );
            } else if ( VersionUtils.satisfiesVersion( apiVersion, "1.0.0" ) ) {
                // this is a pre-1.5 workaround that reproduces the result of the query by intersecting the output
                // of two endpoints

                // request 1 (nameLike)
                URI uri1 = UriComponentsBuilder.fromUri( apiUri )
                        .replaceQueryParam( "descriptionLike" )
                        .build().toUri();
                CompletableFuture<ResponseEntity<User[]>> nameLikeFuture = asyncRestTemplate.getForEntity( uri1, User[].class ).completable();

                // request 2 (descriptionLike)
                URI uri2 = UriComponentsBuilder.fromUri( apiUri )
                        .replaceQueryParam( "nameLike" )
                        .replaceQueryParam( "prefix" )
                        .build().toUri();
                CompletableFuture<ResponseEntity<User[]>> descriptionLikeFuture = asyncRestTemplate.getForEntity( uri2, User[].class ).completable();

                return nameLikeFuture.thenCombine( descriptionLikeFuture, ( a, b ) -> {
                    if ( a.getBody() == null || b.getBody() == null ) {
                        log.warn( String.format( "One or both of these responses had a null body: %s, %s. Nothing will be returned for the intersection.", a, b ) );
                        return new ResponseEntity<>( new User[0], a.getHeaders(), a.getStatusCode() );
                    }
                    Set<User> nameLikeUsers = new HashSet<>( Arrays.asList( a.getBody() ) );
                    Set<User> descriptionLikeUsers = new HashSet<>( Arrays.asList( b.getBody() ) );
                    nameLikeUsers.retainAll( descriptionLikeUsers );
                    return new ResponseEntity<>( nameLikeUsers.toArray( new User[0] ), a.getHeaders(), a.getStatusCode() );
                } );
            } else {
                return null;
            }
        };
        return getRemoteEntities( User[].class, SEARCH_USERS_PATH, params, requestFilters ).stream()
                .map( this::initUser )
                .sorted( User.getComparator() )
                .collect( Collectors.toList() );
    }


    @Override
    public List<UserGene> findGenesBySymbol( String symbol, Taxon taxon, Set<TierType> tiers, Integer
            orthologTaxonId, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
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
                    .ontologyTermInfos( ontologyTermInfos )
                    .build().toMultiValueMap() );
            RequestFilter<UserGene[]> filters;
            if ( ontologyTermInfos != null ) {
                filters = satisfiesVersion( "1.5.0" );
            } else {
                filters = satisfiesVersion( "1.0.0" );
            }
            intlUsergenes.addAll( getRemoteEntities( UserGene[].class, SEARCH_GENES_PATH, params, filters ) );
        }
        Map<Integer, Integer> taxonOrderingById = taxonService.findByActiveTrue().stream()
                .collect( Collectors.toMap( Taxon::getId, Taxon::getOrdering ) );
        for ( UserGene g : intlUsergenes ) {
            // add back-reference to user
            g.setUser( initUser( g.getRemoteUser() ) );
            // populate taxon ordering
            g.getTaxon().setOrdering( taxonOrderingById.getOrDefault( g.getTaxon().getId(), null ) );
        }
        // sort results from different sources
        return intlUsergenes.stream()
                .sorted( UserGene.getComparator() )
                .collect( Collectors.toList() ); // we need to preserve the search order
    }

    @Override
    public User getRemoteUser( Integer userId, URI remoteHost ) throws RemoteException {
        // Ensure that the remoteHost is one of our known APIs by comparing the URI authority component and always use
        // the URI defined in the configuration
        URI apiUri = getApiUri( remoteHost, true );
        URI uri = UriComponentsBuilder.fromUri( apiUri )
                .path( GET_USER_PATH )
                .build( Collections.singletonMap( "userId", userId ) );

        return getUserByUri( uri );
    }

    @Override
    public User getAnonymizedUser( UUID anonymousId, URI remoteHost ) throws RemoteException {
        URI apiUri = getApiUri( remoteHost, true );

        // the currentProxy() is necessary to have the result cached
        if ( !VersionUtils.satisfiesVersion( remoteResourceService.getApiVersion( remoteHost ), "1.4.0" ) ) {
            log.info( MessageFormat.format( "{0} does not support retrieving user by anonymous identifier, will return null instead.", remoteHost ) );
            return null;
        }

        URI uri = UriComponentsBuilder.fromUri( apiUri )
                .path( GET_USER_BY_ANONYMOUS_ID_PATH )
                .buildAndExpand( Collections.singletonMap( "anonymousId", anonymousId ) )
                .toUri();

        return getUserByUri( uri );
    }

    @Override
    public Future<List<RemoteOntologyTermInfo>> getTermsByOntologyNameAndTerms( Ontology ontology, Collection<OntologyTermInfo> terms, URI remoteHost ) throws RemoteException {
        List<String> badTerms = terms.stream()
                .filter( t -> !t.getOntology().equals( ontology ) )
                .map( OntologyTermInfo::toString )
                .collect( Collectors.toList() );
        if ( !badTerms.isEmpty() ) {
            throw new IllegalArgumentException( String.format( "The following terms are not part of %s: %s.", String.join( ", ", badTerms ), ontology ) );
        }
        if ( !VersionUtils.satisfiesVersion( remoteResourceService.getApiVersion( remoteHost ), "1.5.0" ) ) {
            return CompletableFuture.completedFuture( null );
        }
        List<String> termIds = terms.stream()
                .map( OntologyTermInfo::getTermId )
                .collect( Collectors.toList() );
        URI uri = UriComponentsBuilder.fromUri( getApiUri( remoteHost, false ) )
                .path( GET_ONTOLOGY_TERMS_PATH )
                .queryParam( "ontologyTermIds", termIds )
                .build( ontology.getName() );
        return asyncRestTemplate.getForEntity( uri, RemoteOntologyTermInfo[].class ).completable()
                .handle( ( re, ex ) -> {
                    if ( ex != null ) {
                        if ( ex instanceof HttpClientErrorException && ( (HttpClientErrorException) ex ).getStatusCode() == HttpStatus.NOT_FOUND ) {
                            return null;
                        } else {
                            log.warn( String.format( "Failed to retrieve ontology terms from %s: %s", uri, ExceptionUtils.getRootCauseMessage( ex ) ) );
                            throw new RuntimeException( ex );
                        }
                    } else if ( re.getBody() == null ) {
                        log.warn( String.format( "Invalid response for %s: the body is null.", uri ) );
                        return null;
                    } else {
                        return Arrays.asList( re.getBody() );
                    }
                } );
    }

    @Override
    public List<URI> getApiUris() {
        return getApiUris( false );
    }


    private User getUserByUri( URI uri ) throws RemoteException {
        return initUser( getFromRequestFuture( uri, asyncRestTemplate.getForEntity( uri, User.class ) ) );
    }

    /**
     * Retrieve entities from all registered partner APIs.
     *
     * @param arrCls        the type of entities to retrieve as a {@link Class}
     * @param path          the API endpoint to query
     * @param params        the initial API parameters
     * @param requestFilter a {@link RequestFilter} to apply on the above parameters to ultimately produce the
     *                      desired entity
     * @param <T>           the type of entities to retrieve
     * @return the entities from all registered partner APIs
     */
    private <T> Collection<T> getRemoteEntities( Class<T[]> arrCls, String path, MultiValueMap<String, String> params, RequestFilter<T[]> requestFilter ) {
        // it's important to collect, otherwise the future will be created and joined one-by-one, defeating the purpose of using them in the first place
        List<Pair<URI, Future<ResponseEntity<T[]>>>> uriAndFutures = getApiUris( true ).stream()
                .map( uri -> UriComponentsBuilder.fromUri( uri )
                        .path( path )
                        .queryParams( params )
                        .build().toUri() )
                .map( uri -> {
                    Future<ResponseEntity<T[]>> entity = requestFilter.filter( uri, ( apiUri, next ) -> asyncRestTemplate.getForEntity( apiUri, arrCls ) );
                    return entity == null ? null : Pair.of( uri, entity );
                } )
                .filter( Objects::nonNull )
                .collect( Collectors.toList() );

        List<T> entities = new ArrayList<>();
        for ( Pair<URI, Future<ResponseEntity<T[]>>> f : uriAndFutures ) {
            try {
                entities.addAll( Arrays.asList( getFromRequestFuture( f.getLeft(), f.getRight() ) ) );
            } catch ( RemoteException remoteException ) {
                if ( isMissingOntologyException( remoteException ) ) {
                    // this is producing a 400 Bad Request, but we can safely ignore it and treat it as producing no
                    // results
                    log.debug( String.format( "Partner API %s is missing some of the requested ontologies: %s", f.getLeft(), remoteException.getMessage() ) );
                } else {
                    log.warn( String.format( "Failed to retrieve entity from %s : %s", f.getLeft(), ExceptionUtils.getRootCauseMessage( remoteException ) ) );
                }
            }
        }

        return entities;
    }

    private static boolean isMissingOntologyException( RemoteException remoteException ) {
        if ( remoteException.getCause() instanceof HttpClientErrorException ) {
            HttpClientErrorException httpException = (HttpClientErrorException) remoteException.getCause();
            return httpException.getStatusCode() == HttpStatus.BAD_REQUEST && httpException.getResponseBodyAsString().startsWith( "The following ontologies do not exist in this registry:" );
        }
        return false;
    }

    private <T> T getFromRequestFuture( URI uri, Future<ResponseEntity<T>> future ) throws RemoteException {
        try {
            T entity = future.get().getBody();
            if ( entity == null ) {
                throw new RemoteException( String.format( "Invalid response for %s: the body is null.", uri ) );
            }
            return entity;
        } catch ( ExecutionException e ) {
            throw new RemoteException( String.format( "Unsuccessful response received for %s.", uri ), e.getCause() );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RemoteException( String.format( "A thread was interrupted while waiting for %s response.", uri ), e );
        }
    }

    /**
     * Obtain an API URI given a remote host with the option of preserving authentication information.
     *
     * @param remoteHost   remote host used to match one of the configured API URI
     * @param authenticate authenticate the given URI if the current user is an administrator
     * @return an API URI configured with authentication if authenticate is true and the current user is an
     * administrator
     * @throws UnknownRemoteApiException if no API URI can be found matching the remote host
     */
    private URI getApiUri( URI remoteHost, boolean authenticate ) throws UnknownRemoteApiException {
        String remoteHostAuthority = remoteHost.getAuthority();
        Map<String, URI> apiUriByAuthority = Arrays.stream( applicationSettings.getIsearch().getApis() )
                .collect( Collectors.toMap( URI::getAuthority, identity() ) );
        URI apiUri = apiUriByAuthority.get( remoteHostAuthority );
        if ( apiUri == null ) {
            throw new UnknownRemoteApiException( remoteHost );
        }
        return prepareApiUri( apiUri, authenticate );
    }

    private List<URI> getApiUris( boolean authenticate ) {
        return Arrays.stream( applicationSettings.getIsearch().getApis() )
                .map( apiUri -> prepareApiUri( apiUri, authenticate ) )
                .collect( Collectors.toList() );
    }

    private URI prepareApiUri( URI apiUri, boolean authenticate ) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromUri( apiUri )
                .replaceQueryParam( "auth" )
                .replaceQueryParam( "noauth" );
        if ( authenticate ) {
            User user = userService.findCurrentUser();
            if ( user != null && user.getRoles().contains( roleRepository.findByRole( "ROLE_ADMIN" ) ) ) {
                UriComponents apiUriComponents = UriComponentsBuilder.fromUri( apiUri ).build();
                //noinspection StatementWithEmptyBody
                if ( apiUriComponents.getQueryParams().containsKey( "noauth" ) ) {
                    // do nothing, we don't have admin access for this partner
                } else if ( apiUriComponents.getQueryParams().containsKey( "auth" ) ) {
                    // use a specific search token
                    uri.queryParam( "auth", apiUriComponents.getQueryParams().getFirst( "auth" ) );
                } else if ( applicationSettings.getIsearch().getSearchToken() != null ) {
                    // use the default search token
                    uri.queryParam( "auth", applicationSettings.getIsearch().getSearchToken() );
                }
            }
        }
        return uri.build().toUri();
    }

    private User initUser( User user ) {
        user.setEnabled( true );
        user.getUserGenes().values().forEach( ug -> ug.setUser( user ) );
        user.getUserTerms().forEach( ug -> ug.setUser( user ) );
        user.getUserOrgans().values().forEach( ug -> ug.setUser( user ) );
        return user;
    }

    private SortedSet<TierType> restrictTiers( Set<TierType> tiers ) {
        return tiers.stream()
                .filter( t -> t != TierType.TIER3 )
                .collect( Collectors.toCollection( TreeSet::new ) );
    }

    /**
     * Interface used to construct a filter chain.
     *
     * @param <T> the type of entities that is retrieved from the endpoint
     */
    @FunctionalInterface
    private interface RequestFilter<T> {

        /**
         * @param apiUri the API URI
         * @return the result of the step, or null to abort the chain
         * @
         */
        Future<ResponseEntity<T>> filter( URI apiUri, RequestFilter<T> next );
    }

    /**
     * Filter that checks if and endpoint satisfies a given API version requirement.
     */
    private <T> RequestFilter<T> satisfiesVersion( String minimumVersion ) {
        return ( apiUri, next ) -> {
            String apiVersion;
            try {
                apiVersion = remoteResourceService.getApiVersion( apiUri );
            } catch ( RemoteException e ) {
                log.warn( String.format( "Failed to retrieve API version from %s: %s", apiUri, ExceptionUtils.getRootCauseMessage( e ) ) );
                return null;
            }
            if ( VersionUtils.satisfiesVersion( apiVersion, minimumVersion ) ) {
                return next.filter( apiUri, null );
            } else {
                return null;
            }
        };
    }

    @Data
    @SuperBuilder
    private static class UserSearchParams {

        private Collection<ResearcherPosition> researcherPositions;
        private Collection<ResearcherCategory> researcherCategories;
        private Collection<String> organUberonIds;
        private Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos;

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
            if ( ontologyTermInfos != null ) {
                for ( Map.Entry<Ontology, Set<OntologyTermInfo>> entry : ontologyTermInfos.entrySet() ) {
                    for ( OntologyTermInfo termInfo : entry.getValue() ) {
                        params.add( "ontologyNames", entry.getKey().getName() );
                        params.add( "ontologyTermIds", termInfo.getTermId() );
                    }
                }
            }
            return params;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @SuperBuilder
    private static class UserGeneSearchParams extends UserSearchParams {

        private Integer taxonId;
        private TierType tier;
        private Integer orthologTaxonId;

        @Override
        public MultiValueMap<String, String> toMultiValueMap() {
            MultiValueMap<String, String> params = super.toMultiValueMap();
            params.add( "taxonId", taxonId.toString() );
            params.add( "tier", tier.name() );
            if ( orthologTaxonId != null ) {
                params.add( "orthologTaxonId", orthologTaxonId.toString() );
            }
            return params;
        }

    }

}
