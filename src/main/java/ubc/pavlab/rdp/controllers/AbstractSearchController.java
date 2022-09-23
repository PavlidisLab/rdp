package ubc.pavlab.rdp.controllers;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubc.pavlab.rdp.exception.RemoteException;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.RemoteResource;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.model.ontology.RemoteOntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.services.OrganInfoService;
import ubc.pavlab.rdp.services.RemoteResourceService;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Holds common implementation details of {@link SearchController} and {@link SearchViewController}.
 *
 * @author poirigui
 * @see SearchController
 * @see SearchViewController
 */
@CommonsLog
public abstract class AbstractSearchController {

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private RemoteResourceService remoteResourceService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    protected static class SearchParams {
        private boolean iSearch;
        private Set<ResearcherPosition> researcherPositions;
        private Set<ResearcherCategory> researcherCategories;
        private Set<String> organUberonIds;
        /**
         * Order matters here because we want to preserve the UI rendering.
         */
        private List<Integer> ontologyTermIds;

        /**
         * Indicate of the search parameters are empty.
         */
        public boolean isEmpty() {
            return researcherPositions == null && researcherCategories == null && organUberonIds == null && ontologyTermIds == null;
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    protected static class UserSearchParams extends SearchParams {
        @NotNull
        private String nameLike;
        private boolean prefix;
        @NotNull
        private String descriptionLike;

        public UserSearchParams( String nameLike, boolean prefix, String descriptionLike, boolean iSearch, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, List<Integer> ontologyTermIds ) {
            super( iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds );
            this.nameLike = nameLike;
            this.prefix = prefix;
            this.descriptionLike = descriptionLike;
        }

        @Override
        public boolean isEmpty() {
            return nameLike.isEmpty() && descriptionLike.isEmpty() && super.isEmpty();
        }
    }

    protected static class UserSearchParamsValidator implements Validator {

        @Override
        public boolean supports( Class<?> clazz ) {
            return UserSearchParams.class.isAssignableFrom( clazz );
        }

        @Override
        public void validate( Object target, Errors errors ) {
            UserSearchParams userSearchParams = (UserSearchParams) target;
            if ( userSearchParams.isEmpty() ) {
                errors.reject( "AbstractSearchController.UserSearchParams.emptyQueryNotAllowed" );
            }
        }
    }

    @InitBinder("userSearchParams")
    public void initBinder( WebDataBinder webDataBinder ) {
        webDataBinder.addValidators( new UserSearchParamsValidator() );
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    protected static class GeneSearchParams extends SearchParams {
        @NotNull
        private GeneInfo gene;
        private Set<TierType> tiers;
        private Taxon orthologTaxon;

        public GeneSearchParams( GeneInfo gene, Set<TierType> tiers, Taxon orthologTaxon, boolean iSearch, Set<ResearcherPosition> researcherPositions, Set<ResearcherCategory> researcherCategories, Set<String> organUberonIds, List<Integer> ontologyTermIds ) {
            super( iSearch, researcherPositions, researcherCategories, organUberonIds, ontologyTermIds );
            this.gene = gene;
            this.tiers = tiers;
            this.orthologTaxon = orthologTaxon;
        }

        @Override
        public boolean isEmpty() {
            return tiers.isEmpty() && super.isEmpty();
        }
    }

    private static void addClause( StringBuilder builder, List<String> clauses ) {
        if ( builder.length() > 0 ) {
            builder.append( " AND " );
        }
        if ( clauses.size() > 1 ) {
            builder.append( '(' );
        }
        builder.append( String.join( " OR ", clauses ) );
        if ( clauses.size() > 1 ) {
            builder.append( ')' );
        }
    }

    private static void addClause( StringBuilder builder, String clause ) {
        addClause( builder, Collections.singletonList( clause ) );
    }

    private void addSearchParams( StringBuilder builder, SearchParams userSearchParams, Locale locale ) {
        if ( userSearchParams.researcherPositions != null ) {
            addClause( builder, userSearchParams.researcherPositions.stream()
                    .map( p -> String.format( locale, "'Researcher Position' = '%s'",
                            messageSource.getMessage( p.getResolvableTitle(), locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.researcherCategories != null ) {
            addClause( builder, userSearchParams.researcherCategories.stream()
                    .map( p -> String.format( locale, "'Researcher Category' = '%s'",
                            messageSource.getMessage( p.getResolvableTitle(), locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.organUberonIds != null ) {
            addClause( builder, organInfoService.findByUberonIdIn( userSearchParams.organUberonIds ).stream()
                    .map( p -> String.format( locale, "'Human Organ System' = '%s'", p.getName() ) ).collect( Collectors.toList() ) );
        }
        if ( userSearchParams.ontologyTermIds != null ) {
            List<OntologyTermInfo> infos = ontologyService.findAllTermsByIdIn( userSearchParams.ontologyTermIds );
            TreeMap<Ontology, List<OntologyTermInfo>> termByOntology = infos.stream()
                    .sorted()
                    .collect( Collectors.groupingBy( OntologyTermInfo::getOntology, TreeMap::new, Collectors.toList() ) );
            for ( Ontology ontology : termByOntology.navigableKeySet() ) {
                List<OntologyTermInfo> terms = termByOntology.get( ontology );
                addClause( builder, terms.stream()
                        .map( p -> String.format( locale, "'%s' = '%s'",
                                messageSource.getMessage( ontology.getResolvableTitle(), locale ),
                                messageSource.getMessage( p.getResolvableTitle(), locale ) ) )
                        .collect( Collectors.toList() ) );
            }
        }
    }

    protected String summarizeUserSearchParams( UserSearchParams userSearchParams, Locale locale ) {
        StringBuilder builder = new StringBuilder();
        if ( !userSearchParams.nameLike.isEmpty() ) {
            addClause( builder, String.format( "'Name' %s '%s'", userSearchParams.prefix ? "starts with" : "contains", userSearchParams.nameLike ) );
        }
        if ( !userSearchParams.descriptionLike.isEmpty() ) {
            addClause( builder, String.format( "'Research Description' contains '%s'", userSearchParams.descriptionLike ) );
        }
        addSearchParams( builder, userSearchParams, locale );
        return "Search query: " + builder + ".";
    }

    protected String summarizeGeneSearchParams( GeneSearchParams geneSearchParams, Locale locale ) {
        StringBuilder builder = new StringBuilder();
        addClause( builder, String.format( "'Gene Symbol' = '%s'", geneSearchParams.gene.getSymbol() ) );
        addSearchParams( builder, geneSearchParams, locale );
        return "Filters applied: " + builder + ".";
    }

    /**
     * Rewrite the search parameters to be more specific when either the name or description patterns are missing.
     */
    protected String redirectToSpecificSearch( String nameLike, String descriptionLike ) {
        if ( nameLike.isEmpty() == descriptionLike.isEmpty() ) {
            throw new IllegalArgumentException( "Either of 'nameLike' or 'descriptionLike' has to be empty, but not both." );
        }
        if ( nameLike.isEmpty() ) {
            return "redirect:" + ServletUriComponentsBuilder.fromCurrentRequest()
                    .scheme( null ).host( null )
                    .replaceQueryParam( "nameLike" )
                    .replaceQueryParam( "prefix" )
                    .build( true )
                    .toUriString();
        } else {
            return "redirect:" + ServletUriComponentsBuilder.fromCurrentRequest()
                    .scheme( null ).host( null )
                    .replaceQueryParam( "descriptionLike" )
                    .build( true )
                    .toUriString();
        }
    }

    /**
     * Summarizes the availability of an ontology in a given partner API.
     */
    @Value
    protected static class OntologyAvailability {
        String origin;
        URI originUrl;
        Ontology ontology;
        boolean available;
        List<OntologyTermInfo> availableTerms;
        List<OntologyTermInfo> missingTerms;
    }

    /**
     * Obtain a summary of available and missing ontologies and terms among partner APIs given a set of local terms.
     */
    protected Map<URI, List<OntologyAvailability>> getOntologyAvailabilityByApiUri( List<Integer> ontologyTermIds ) {
        Map<Ontology, List<OntologyTermInfo>> termsByOntology = ontologyService.findAllTermsByIdIn( ontologyTermIds ).stream()
                .collect( Collectors.groupingBy( OntologyTermInfo::getOntology, Collectors.toList() ) );
        // FIXME: should we handle the case where some terms are not matched?
        Map<Pair<URI, Ontology>, Future<List<RemoteOntologyTermInfo>>> futures = new HashMap<>();
        for ( URI remoteHost : remoteResourceService.getApiUris() ) {
            for ( Map.Entry<Ontology, List<OntologyTermInfo>> entry : termsByOntology.entrySet() ) {
                try {
                    futures.put( Pair.of( remoteHost, entry.getKey() ), remoteResourceService.getTermsByOntologyNameAndTerms( entry.getKey(), entry.getValue(), remoteHost ) );
                } catch ( RemoteException ex ) {
                    // defer the handling of the exception
                    CompletableFuture<List<RemoteOntologyTermInfo>> future = new CompletableFuture<>();
                    future.completeExceptionally( ex );
                    futures.put( Pair.of( remoteHost, entry.getKey() ), future );
                }
            }
        }

        Map<Pair<URI, Ontology>, ExecutionException> exceptions = new HashMap<>();
        Map<Pair<URI, Ontology>, List<RemoteOntologyTermInfo>> availableTermsByApiUri;
        try {
            availableTermsByApiUri = collectAsManyFuturesAsPossible( futures, exceptions );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }

        for ( Map.Entry<Pair<URI, Ontology>, ExecutionException> entry : exceptions.entrySet() ) {
            ExecutionException e = entry.getValue();
            if ( e.getCause() instanceof RemoteException ) {
                log.warn( String.format( "Failed to collect future from %s: %s", entry.getKey().getLeft(), ExceptionUtils.getRootCauseMessage( entry.getValue() ) ) );
            } else {
                // a non-remote exception is actually pretty serious
                log.error( String.format( "Failed to collect future from %s: %s", entry.getKey().getLeft(), e.getCause().getMessage() ), e );
            }
        }

        Map<URI, List<OntologyAvailability>> availability = new HashMap<>();
        for ( URI remoteHost : remoteResourceService.getApiUris() ) {
            List<OntologyAvailability> availabilities = new ArrayList<>( termsByOntology.size() );
            // lookup origin and origin URL in all terms from all ontologies
            // when an ontology is not present, there are no terms returned and thus no way to determine the origin, so
            // we increase our chance by of finding something useful this way
            // if we cannot find those in any way, resort to guessing by performing various queries
            RemoteResource rr = availableTermsByApiUri.entrySet().stream()
                    .filter( entry -> entry.getKey().getLeft().equals( remoteHost ) )
                    .filter( entry -> entry.getValue() != null )
                    .flatMap( entry -> entry.getValue().stream() )
                    .findAny().orElse( null );
            // this is a last resort attempt by guessing the RemoteResource from the OpenAPI spec
            if ( rr == null ) {
                try {
                    rr = remoteResourceService.getRepresentativeRemoteResource( remoteHost );
                } catch ( RemoteException e ) {
                    log.warn( String.format( "Failed to retrieve a representative RemoteResource for %s: %s", remoteHost, ExceptionUtils.getRootCauseMessage( e ) ) );
                    continue;
                }
            }
            String origin = rr.getOrigin();
            URI originUrl = rr.getOriginUrl();
            for ( Map.Entry<Ontology, List<OntologyTermInfo>> entry : termsByOntology.entrySet() ) {
                Ontology ontology = entry.getKey();
                List<OntologyTermInfo> terms = entry.getValue();
                Pair<URI, Ontology> key = Pair.of( remoteHost, ontology );
                // in case of an exception, we cannot not for sure that the ontology is unavailable
                boolean ontologyAvailable = availableTermsByApiUri.get( key ) != null;
                List<OntologyTermInfo> availableTerms = new ArrayList<>();
                List<OntologyTermInfo> missingTerms = new ArrayList<>();
                if ( ontologyAvailable ) {
                    for ( OntologyTermInfo term : terms ) {
                        if ( availableTermsByApiUri.get( key ).stream()
                                .map( OntologyTerm::getTermId )
                                .anyMatch( t -> t.equals( term.getTermId() ) ) ) {
                            availableTerms.add( term );
                        } else {
                            missingTerms.add( term );
                        }
                    }
                } else {
                    missingTerms.addAll( terms );
                }
                availabilities.add( new OntologyAvailability( origin, originUrl, ontology, ontologyAvailable, availableTerms, missingTerms ) );
            }

            // ignore partner APIs that have all the requested terms or for which the request failed
            availabilities.removeIf( a -> ( a.available && a.missingTerms.isEmpty() ) || exceptions.containsKey( Pair.of( remoteHost, a.ontology ) ) );

            if ( !availabilities.isEmpty() ) {
                availability.put( remoteHost, availabilities );
            }
        }

        return availability;
    }

    /**
     * Collect as many futures as possible, organized in a mapping.
     *
     * @param futures             the futures to collect
     * @param executionExceptions a destination for caught {@link ExecutionException}, or null to ignore
     * @param <K>                 the type of the key in the future mapping
     * @param <T>                 the type of the future
     * @return collected future results
     * @throws InterruptedException if the current thread was interrupted while waiting on a future to complete, see
     *                              {@link Future#get()}.
     * @see Future#get()
     */
    private static <K, T> Map<K, T> collectAsManyFuturesAsPossible( Map<K, ? extends Future<T>> futures, Map<K, ExecutionException> executionExceptions ) throws InterruptedException {
        Map<K, T> results = new HashMap<>();
        for ( Map.Entry<K, ? extends Future<T>> entry : futures.entrySet() ) {
            try {
                results.put( entry.getKey(), entry.getValue().get() );
            } catch ( ExecutionException e ) {
                executionExceptions.put( entry.getKey(), e );
            }
        }
        return results;
    }

    protected Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }

    /**
     * No need to perform the same
     *
     * @param ontologyTermIds
     * @return
     */
    protected Map<Ontology, Set<OntologyTermInfo>> ontologyTermsFromIds( List<Integer> ontologyTermIds ) {
        return ontologyTermIds == null ? null : ontologyService.findAllTermsByIdIn( ontologyTermIds ).stream()
                .collect( Collectors.groupingBy( OntologyTermInfo::getOntology, Collectors.toSet() ) );
    }
}
