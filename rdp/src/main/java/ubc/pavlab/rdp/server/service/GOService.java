/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.service;

import java.util.Collection;
import java.util.Map;

import org.json.JSONArray;
import org.springframework.beans.factory.InitializingBean;

import ubc.pavlab.rdp.server.model.GOTerm;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface GOService extends InitializingBean {
    public abstract Map<GOTerm, Long> calculateGoTermFrequency( Collection<Gene> genes, Long taxonId,
            int minimumFrequency, int minimumTermSize, int maximumTermSize );

    public abstract Collection<GeneOntologyTerm> fetchByQuery( String queryString );

    /**
     * @param entry
     * @return children, NOT including part-of relations.
     */
    public abstract Collection<GOTerm> getDescendants( GOTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return
     */
    public abstract Collection<GOTerm> getDescendants( GOTerm entry, boolean includePartOf );

    /**
     * @param entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *        included incidentally if they are parents of other terms in the collection.
     * @return
     */
    public abstract Collection<GOTerm> getAncestors( GOTerm entry );

    /**
     * @param entries
     * @param includePartOf
     * @return
     */
    public abstract Collection<GOTerm> getAncestors( GOTerm entry, boolean includePartOf );

    public abstract Collection<GOTerm> getChildren( GOTerm entry );

    public abstract Collection<GOTerm> getChildren( GOTerm entry, boolean includePartOf );

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     * 
     * @param entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    public abstract Collection<GOTerm> getParents( GOTerm entry );

    /**
     * @param entry
     * @param includePartOf
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     *         included in the returned information
     */
    public abstract Collection<GOTerm> getParents( GOTerm entry, boolean includePartOf );

    public abstract Collection<Gene> getGenes( String id, Long taxonId );

    public abstract Collection<Gene> getGenes( GOTerm t, Long taxonId );

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    public abstract Collection<GOTerm> getGOTerms( Gene gene );

    /**
     * Get all GO terms for a gene, including parents of terms via is-a relationships; and optionally also parents via
     * part-of relationships.
     * 
     * @param gene
     * @param includePartOf
     * @return
     */
    public abstract Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf );

    public abstract Collection<GOTerm> getGOTerms( Gene gene, boolean includePartOf, boolean propagateUpwards );

    public abstract Long getGeneSize( GOTerm t );

    public abstract Long getDirectGeneSize( GOTerm t );

    public abstract Long getGeneSizeInTaxon( String id, Long taxonId );

    public abstract Long getGeneSizeInTaxon( GOTerm t, Long taxonId );

    public abstract Long getDirectGeneSizeInTaxon( GOTerm t, Long taxonId );

    public abstract Collection<Gene> getRelatedGenes( Collection<GeneOntologyTerm> goTerms, Long taxonId );

    public abstract Long computeOverlapFrequency( String id, Collection<Gene> genes );

    public abstract void init();

    public abstract Collection<GeneOntologyTerm> deserializeGOTerms( String[] GOJSON );

    public abstract JSONArray toJSON( Collection<GeneOntologyTerm> goTerms );

}
