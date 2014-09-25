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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;

import org.springframework.stereotype.Component;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.util.SearchableEhcache;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Component
public class NcbiCacheImpl extends SearchableEhcache<Gene> implements NcbiCache {
    // These constants are used in ehcache.xml. If they are changed, ehcache.xml must be modified.
    private static final String CACHE_NAME = "NcbiCache";
    private static final String GENE_NAME_SEARCH_ATTRIBUTE_NAME = "officialName";
    private static final String GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME = "officialSymbol";
    private static final String TAXON_SEARCH_ATTRIBUTE_NAME = "taxon";
    private static final String GENE_NCBI_ID_SEARCH_ATTRIBUTE_NAME = "ncbiGeneId";
    private static final String ALIASES_ATTRIBUTE_NAME = "aliases";

    private Attribute<Object> geneNameAttribute;
    private Attribute<Object> geneSymbolAttribute;
    private Attribute<Object> taxonAttribute;
    private Attribute<Object> geneNcbiIdAttribute;
    private Attribute<Object> aliasesAttribute;

    @Override
    public Collection<Gene> fetchGenesByGeneSymbolsAndTaxon( Collection<String> geneSymbols, String taxon ) {
        Criteria symbolCriteria = geneSymbolAttribute.in( geneSymbols );
        Criteria taxonCriteria = taxonAttribute.eq( taxon );

        return fetchByCriteria( taxonCriteria.and( symbolCriteria ) );
    }

    @Override
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) {
        Criteria symbolCriteria = geneSymbolAttribute.in( geneSymbols );

        return fetchByCriteria( symbolCriteria );
    }

    @Override
    public Collection<Gene> fetchGenesByGeneTaxon( Collection<String> taxons ) {
        Criteria taxonCriteria = taxonAttribute.in( taxons );

        return fetchByCriteria( taxonCriteria );
    }

    /**
     * See Bug 4187 - Improved gene search
     * 
     * <pre>
     * Sort search results using this order : 
     * 1. Gene symbols. Place exact matches first. 
     * 2. Gene name 
     * 3. Gene alias. Since this are concatenated, we loosely use *SYMBOL*
     * </pre>
     */
    @Override
    public Collection<Gene> findGenes( String queryString, String taxon ) {

        ArrayList<Gene> results = new ArrayList<>();

        Criteria taxonCriteria = taxonAttribute.eq( taxon );

        // 1. Exact Gene Symbols
        String regexQueryString = queryString;
        Criteria symbolCriteria = geneSymbolAttribute.ilike( regexQueryString );
        results.addAll( fetchByCriteria( taxonCriteria.and( symbolCriteria ) ) );

        // 2. Prefix Gene Symbols
        regexQueryString = queryString + "*";
        symbolCriteria = geneSymbolAttribute.ilike( regexQueryString );
        Collection<Gene> tmp = fetchByCriteria( taxonCriteria.and( symbolCriteria ) );
        tmp.removeAll( results );
        results.addAll( tmp );

        // 3. Prefix Gene name
        Criteria nameCriteria = geneNameAttribute.ilike( regexQueryString );
        tmp = fetchByCriteria( taxonCriteria.and( nameCriteria ) );
        tmp.removeAll( results );
        results.addAll( tmp );

        // 4. Gene Alias. Special case, these are concatenated by ','
        // see GeneAliasAttributeExtractor
        regexQueryString = "*" + queryString + "*";
        Criteria aliasesCriteria = aliasesAttribute.ilike( regexQueryString );
        tmp = fetchByCriteria( taxonCriteria.and( aliasesCriteria ) );
        tmp.removeAll( results );
        results.addAll( tmp );

        return results;
    }

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public List<Gene> getGenes( List<String> geneStrings ) {
        List<Gene> genes = new ArrayList<>( geneStrings.size() );

        for ( String geneString : geneStrings ) {
            Criteria symbolCriteria = geneSymbolAttribute.ilike( geneString );
            Criteria geneNcbiIdCriteria = geneNcbiIdAttribute.ilike( geneString );
            Collection<Gene> fetchedGenes = fetchByCriteria( symbolCriteria.or( geneNcbiIdCriteria ) );
            if ( fetchedGenes.size() > 0 ) {
                // Only use the first gene.
                genes.add( fetchedGenes.iterator().next() );
            } else {
                genes.add( null );
            }
        }

        return genes;
    }

    @Override
    public Object getKey( Gene gene ) {
        return gene.getNcbiGeneId();
    }

    @Override
    public void clearAll() {
        removeAll();
    }

    @Override
    public int size() {
        return getSize();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        geneNameAttribute = getSearchAttribute( GENE_NAME_SEARCH_ATTRIBUTE_NAME );
        geneSymbolAttribute = getSearchAttribute( GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME );
        taxonAttribute = getSearchAttribute( TAXON_SEARCH_ATTRIBUTE_NAME );
        geneNcbiIdAttribute = getSearchAttribute( GENE_NCBI_ID_SEARCH_ATTRIBUTE_NAME );
        aliasesAttribute = getSearchAttribute( ALIASES_ATTRIBUTE_NAME );
    }

}
