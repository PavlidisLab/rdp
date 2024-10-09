package ubc.pavlab.rdp.services;

import org.springframework.lang.Nullable;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface TaxonService {

    @Nullable
    Taxon findById( final Integer id );

    Collection<Taxon> findByActiveTrue();
}
