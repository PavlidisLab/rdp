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

package ubc.pavlab.rdp.server.ncbi;

import java.util.Collection;
import java.util.List;

import ubc.pavlab.rdp.server.model.Gene;

/**
 * Stores data queried from NCBI's E-Utils in memory.
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface NcbiCache {
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols );

    public Collection<Gene> fetchGenesByGeneTaxon( Collection<String> taxons );

    public Collection<Gene> findGenes( String queryString, String taxon );

    /**
     * Get a list of genes using the given gene symbols or ncbi ids. The order of the returned list of genes is
     * preserved. If a gene symbol or ncbi id is not valid, the returned gene will be null.
     * 
     * @param geneStrings gene symbols or ncbi ids
     * @return a list of Genes
     */
    public List<Gene> getGenes( List<String> geneStrings );

    public boolean hasExpired();

    public void putAll( Collection<Gene> genes );

    /**
     * Get a list of genes that have an exact matching gene symbol and taxon.
     * 
     * @param geneSymbols
     * @param taxon
     * @return
     */
    public Collection<Gene> fetchGenesByGeneSymbolsAndTaxon( Collection<String> geneSymbols, String taxon );

    public void clearAll();

    public int size();

}
