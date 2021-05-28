package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GOService {

    GeneOntologyTermInfo save( GeneOntologyTermInfo term );

    Iterable<GeneOntologyTermInfo> save( Iterable<GeneOntologyTermInfo> terms );

    GeneOntologyTermInfo saveAlias( String goId, GeneOntologyTermInfo term );

    Iterable<GeneOntologyTermInfo> saveAlias( Map<String, GeneOntologyTermInfo> terms );

    void deleteAll();

    long count();

    Collection<GeneOntologyTermInfo> getDescendants( GeneOntologyTermInfo entry );

    Collection<GeneOntologyTermInfo> getDescendants( GeneOntologyTermInfo entry, boolean includePartOf );

    Map<GeneOntologyTermInfo, Long> termFrequencyMap( Collection<? extends Gene> genes );

    List<SearchResult<GeneOntologyTermInfo>> search( String queryString, Taxon taxon, int max );

    Collection<GeneOntologyTermInfo> getChildren( GeneOntologyTermInfo entry );

    Collection<GeneOntologyTermInfo> getChildren( GeneOntologyTermInfo entry, boolean includePartOf );

    Collection<Integer> getDirectGenes( GeneOntologyTermInfo term );

    Collection<Integer> getGenes( GeneOntologyTermInfo t );

    Collection<Integer> getGenesInTaxon( String id, Taxon taxon );

    Collection<Integer> getGenesInTaxon( GeneOntologyTermInfo t, Taxon taxon );

    Collection<Integer> getGenesInTaxon( Collection<GeneOntologyTermInfo> goTerms, Taxon taxon );

    GeneOntologyTermInfo getTerm( String goId );

    Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene );

    Collection<GeneOntologyTermInfo> getTermsForGene( Gene gene, boolean includePartOf, boolean propagateUpwards );

    long getSizeInTaxon( GeneOntologyTermInfo t, Taxon taxon );

    void updateGoTerms();
}
