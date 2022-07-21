package ubc.pavlab.rdp.controllers;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Holds common implementation details of {@link SearchController} and {@link SearchViewController}.
 *
 * @author poirigui
 * @see SearchController
 * @see SearchViewController
 */
public abstract class AbstractSearchController {

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
}
