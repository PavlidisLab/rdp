package ubc.pavlab.rdp.controllers;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ubc.pavlab.rdp.model.OrganInfo;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.services.OntologyService;
import ubc.pavlab.rdp.services.OrganInfoService;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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

    @Data
    protected static class UserSearchParams {
        @NotNull
        private String nameLike;
        private boolean prefix;
        @NotNull
        private String descriptionLike;
        private boolean iSearch;
        private Set<ResearcherPosition> researcherPositions;
        private Set<ResearcherCategory> researcherCategories;
        private Set<String> organUberonIds;
        /**
         * Order matters here because we want to preserve the UI rendering.
         */
        private List<Integer> ontologyTermIds;
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

    protected Collection<OntologyTermInfo> ontologyTermsFromIds( Collection<Integer> ontologyTermIds ) {
        return ontologyTermIds == null ? null : ontologyService.findAllTermsByIdIn( ontologyTermIds );
    }
}
