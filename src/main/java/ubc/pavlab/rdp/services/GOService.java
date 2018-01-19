package ubc.pavlab.rdp.services;

import org.json.JSONArray;
import org.springframework.beans.factory.InitializingBean;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.GeneOntologyTerm;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.GOTerm;

import java.util.Collection;
import java.util.Map;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GOService extends InitializingBean {
    Map<GOTerm, Long> calculateGoTermFrequency( Collection<Gene> genes, Long taxonId, int minimumFrequency,
                                                int minimumTermSize, int maximumTermSize );

    /**
     * @param entry
     * @return children, NOT including part-of relations.
     */
    Collection<GOTerm> getDescendants( GOTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    Collection<GOTerm> getDescendants( GOTerm entry, boolean includePartOf );

    /**
     * @param entry NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *        included incidentally if they are parents of other terms in the collection.
     * @return
     */
    Collection<GOTerm> getAncestors( GOTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    Collection<GOTerm> getAncestors( GOTerm entry, boolean includePartOf );

    Collection<GOTerm> getChildren( GOTerm entry );

    Collection<GOTerm> getChildren( GOTerm entry, boolean includePartOf );

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     *
     * @param entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    Collection<GOTerm> getParents( GOTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     *         included in the returned information
     */
    Collection<GOTerm> getParents( GOTerm entry, boolean includePartOf );

    Collection<Gene> getGenes( String id, Taxon taxon );

    Collection<Gene> getGenes( GOTerm t, Taxon taxon );

    /**
     * @param gene a gene and return a set of all GO terms including the parents of each GO term
     */
    Collection<GOTerm> getGOTerms( Gene gene );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     *
     * @param gene
     * @param includePartOf
     * @return
     */
    Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf );

    Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf, boolean propagateUpwards );

    Integer getGeneSize( GOTerm t );

    Integer getGeneSizeInTaxon( String id, Taxon taxon );

    Integer getGeneSizeInTaxon( GOTerm t, Taxon taxon );

    Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Taxon taxon );

    Long computeOverlapFrequency( String id, Collection<Gene> genes );

    void init();

    Collection<GOTerm> deserializeGOTerms( String[] GOJSON );

    JSONArray toJSON( Collection<GeneOntologyTerm> goTerms );

    Collection<GOTerm> search( String queryString );
}
