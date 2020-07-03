package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.Organ;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

public interface UserOrganService {

    Collection<Organ> findByDescriptionAndTaxon( String description, Taxon taxon );
}
