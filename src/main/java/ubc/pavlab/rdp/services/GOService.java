package ubc.pavlab.rdp.services;

import org.springframework.scheduling.annotation.Scheduled;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GOService {

    void setTerms( Map<String, GeneOntologyTerm> termMap );

    Collection<GeneOntologyTerm> getAllTerms();

    int size();

    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf );

    Map<GeneOntologyTerm, Long> termFrequencyMap( Collection<GeneInfo> genes );

    List<SearchResult<GeneOntologyTerm>> search( String queryString, Taxon taxon, int max );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf );

    Collection<GeneInfo> getGenes( String id, Taxon taxon );

    Collection<GeneInfo> getGenes( GeneOntologyTerm t, Taxon taxon );

    Collection<GeneInfo> getGenes( GeneOntologyTerm t );

    Collection<GeneInfo> getGenes( Collection<GeneOntologyTerm> goTerms, Taxon taxon );

    GeneOntologyTerm getTerm( String goId );

    void updateGoTerms ();

    Collection<GeneOntologyTerm> getTermsForGene( GeneInfo gene );

    Collection<GeneOntologyTerm> getAllTermsForGene( GeneInfo gene, boolean includePartOf, boolean propagateUpwards );
}
