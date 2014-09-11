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

import ubc.pavlab.rdp.server.exception.NcbiServiceException;
import ubc.pavlab.rdp.server.model.Gene;

/**
 * Used to request gene and other data using NCBI E-Utils.
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface NcbiQueryService {

    /**
     * Find genes by gene symbols filtered by taxon.
     * 
     * @param geneSymbols
     * @param taxon
     * @return
     * @throws NcbiServiceException
     */
    public Collection<Gene> fetchGenesByGeneSymbolsAndTaxon( Collection<String> geneSymbols, String taxon )
            throws NcbiServiceException;

    /**
     * Find genes by gene symbols.
     * 
     * @param geneSymbols
     * @return
     * @throws NcbiServiceException
     */
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) throws NcbiServiceException;

    /**
     * Find genes by tiered approach on symbol, name and alias.
     * 
     * @param queryString
     * @param taxon
     * @return
     * @throws NcbiServiceException
     */
    public Collection<Gene> findGenes( String queryString, String taxon ) throws NcbiServiceException;

    /**
     * Get a list of genes using the given gene symbols or ncbi ids. The order of the returned list of genes is
     * preserved. If a gene symbol or ncbi id is not valid, the returned gene will be null.
     * 
     * @param geneStrings
     * @return
     * @throws NcbiServiceException
     */
    public List<Gene> getGenes( List<String> geneStrings ) throws NcbiServiceException;

}
