package ubc.pavlab.rdp.services;

import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserTerm;

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

    /**
     * @param entry NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *              included incidentally if they are parents of other terms in the collection.
     * @return
     */
    Collection<GeneOntologyTerm> getAncestors( GeneOntologyTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    Collection<GeneOntologyTerm> getAncestors( GeneOntologyTerm entry, boolean includePartOf );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry );

    Collection<GeneOntologyTerm> getChildren( GeneOntologyTerm entry, boolean includePartOf );

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     *
     * @param entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    Collection<GeneOntologyTerm> getParents( GeneOntologyTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     * included in the returned information
     */
    Collection<GeneOntologyTerm> getParents( GeneOntologyTerm entry, boolean includePartOf );

    Collection<Gene> getGenes( String id, Taxon taxon );

    Collection<Gene> getGenes( GeneOntologyTerm t, Taxon taxon );

    /**
     * @param gene a gene and return a set of all GO terms including the parents of each GO term
     */
    Collection<GeneOntologyTerm> getGOTerms( Gene gene );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     *
     * @param gene
     * @param includePartOf
     * @return
     */
    Collection<GeneOntologyTerm> getGOTerms( Gene gene, boolean includePartOf );

    Collection<GeneOntologyTerm> getGOTerms( Gene gene, boolean includePartOf, boolean propagateUpwards );

    Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Taxon taxon );

    Integer computeOverlapFrequency( GeneOntologyTerm t, Set<Gene> genes );

    GeneOntologyTerm getTerm( String goId );

    Collection<UserTerm> convertTermTypes( Collection<GeneOntologyTerm> goTerms, Taxon taxon, Set<Gene> genes );

    List<GeneOntologyTerm> recommendTerms( Collection<Gene> genes );

    List<GeneOntologyTerm> search( String queryString );
}
