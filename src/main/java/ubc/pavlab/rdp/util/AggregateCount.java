package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ubc.pavlab.rdp.model.Taxon;

/**
 * Created by mjacobson on 18/01/18.
 */
@Getter
@AllArgsConstructor
public class AggregateCount {
    private String geneOntologyId;
    private Taxon taxon;
    private long count;
}
