package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GOService {

    Map<String, GeneOntologyTerm> getTerms();

    void setTerms( Map<String, GeneOntologyTerm> termMap );

    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf );

    Map<GeneOntologyTerm, Long> termFrequencyMap( Collection<? extends Gene> genes );

    List<SearchResult<GeneOntologyTerm>> search( String queryString, Taxon taxon, int max );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf );

    Collection<GeneInfo> getGenes( GeneOntologyTerm t );

    Collection<GeneInfo> getGenesInTaxon( String id, Taxon taxon );

    Collection<GeneInfo> getGenesInTaxon( GeneOntologyTerm t, Taxon taxon );

    Collection<GeneInfo> getGenesInTaxon( Collection<GeneOntologyTerm> goTerms, Taxon taxon );

    GeneOntologyTerm getTerm( String goId );

    Collection<GeneOntologyTerm> getTermsForGene( Gene gene );

    Collection<GeneOntologyTerm> getTermsForGene( Gene gene, boolean includePartOf, boolean propagateUpwards );

    void updateGoTerms();
}
