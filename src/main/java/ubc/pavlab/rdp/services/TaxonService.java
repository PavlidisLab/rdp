package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface TaxonService {

    Taxon findById( final Integer id );
    Taxon findByCommonName( final String commonName );
    Collection<Taxon> loadAll();

}
