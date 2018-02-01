package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GOService {
    void setTerms( Map<String, GeneOntologyTerm> termMap );

    Collection<GeneOntologyTerm> getAllTerms();

    int size();


    /**
     * @param entry
     * @return children, NOT including part-of relations.
     */
    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    Collection<GeneOntologyTerm> getDescendants( GeneOntologyTerm entry, boolean includePartOf );

    List<SearchResult<UserTerm>> search( String queryString, Taxon taxon, int max );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf );

    Collection<Gene> getGenes( String id, Taxon taxon );

    Collection<Gene> getGenes( GeneOntologyTerm t, Taxon taxon );

    Collection<Gene> getGenes( GeneOntologyTerm t );

    Collection<UserGene> getRelatedGenes( Collection<? extends GeneOntologyTerm> goTerms, Taxon taxon );

    GeneOntologyTerm getTerm( String goId );

    Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon, Set<Gene> genes );

    UserTerm convertTermTypes( GeneOntologyTerm goTerm, Taxon taxon, Set<Gene> genes );

    List<GeneOntologyTerm> recommendTerms( Collection<Gene> genes );
}
