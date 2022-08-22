package ubc.pavlab.rdp.controllers;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.services.OrganInfoService;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds common implementation details of {@link SearchController} and {@link SearchViewController}.
 *
 * @author poirigui
 * @see SearchController
 * @see SearchViewController
 */
public abstract class AbstractSearchController {

    @Autowired
    private OrganInfoService organInfoService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private MessageSource messageSource;

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
                    .map( p -> String.format( locale, "researcher position = '%s'",
                            messageSource.getMessage( "ResearcherPosition." + p.name(), null, locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.researcherCategories != null ) {
            addClause( builder, userSearchParams.researcherCategories.stream()
                    .map( p -> String.format( locale, "researcher category = '%s'",
                            messageSource.getMessage( "ResearcherCategory." + p.name(), null, locale ) ) )
                    .collect( Collectors.toList() ) );
        }
        if ( userSearchParams.organUberonIds != null ) {
            addClause( builder, organInfoService.findByUberonIdIn( userSearchParams.organUberonIds ).stream()
                    .map( p -> String.format( locale, "UBERON = '%s'", p.getName() ) ).collect( Collectors.toList() ) );
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
            addClause( builder, String.format( "name %s '%s'", userSearchParams.prefix ? "starts with" : "contains", userSearchParams.nameLike ) );
        }
        if ( !userSearchParams.descriptionLike.isEmpty() ) {
            addClause( builder, String.format( "research description contains '%s'", userSearchParams.descriptionLike ) );
        }
        addSearchParams( builder, userSearchParams, locale );
        return builder.toString();
    }

    protected String summarizeGeneSearchParams( GeneSearchParams geneSearchParams, Locale locale ) {
        StringBuilder builder = new StringBuilder();
        addClause( builder, String.format( "gene symbol = '%s'", geneSearchParams.gene.getSymbol() ) );
        if ( geneSearchParams.getTiers() != null ) {
            addClause( builder, geneSearchParams.tiers.stream().map( t -> String.format( "gene tier = '%s'", t.getLabel() ) ).collect( Collectors.toList() ) );
        }
        if ( geneSearchParams.orthologTaxon != null ) {
            addClause( builder, String.format( "gene taxon = '%s'", geneSearchParams.orthologTaxon.getCommonName() ) );
        }
        addSearchParams( builder, geneSearchParams, locale );
        return builder.toString();
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

    protected Collection<OrganInfo> organsFromUberonIds( Set<String> organUberonIds ) {
        return organUberonIds == null ? null : organInfoService.findByUberonIdIn( organUberonIds );
    }

    protected List<OntologyTermInfo> ontologyTermsFromIds( List<Integer> ontologyTermIds ) {
        return ontologyTermIds == null ? null : ontologyService.findAllTermsByIdIn( ontologyTermIds );
    }
}
