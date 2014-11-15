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

package ubc.pavlab.rdp.server.cache;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.PostConstruct;

import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class GeneCacheImpl extends SearchableEhcache<Gene> implements GeneCache {
    // These constants are used in ehcache.xml. If they are changed, ehcache.xml must be modified.
    private static final String CACHE_NAME = "GeneCache";
    private static final String GENE_ID_SEARCH_ATTRIBUTE_NAME = "id";
    private static final String GENE_NAME_SEARCH_ATTRIBUTE_NAME = "officialName";
    private static final String GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME = "officialSymbol";
    private static final String TAXON_SEARCH_ATTRIBUTE_NAME = "taxonId";
    private static final String ALIASES_SEARCH_ATTRIBUTE_NAME = "aliases";
    private static final String MODIFICATION_DATE_SEARCH_ATTRIBUTE_NAME = "modificationDate";

    private Attribute<Object> idAttribute;
    private Attribute<Object> geneNameAttribute;
    private Attribute<Object> geneSymbolAttribute;
    private Attribute<Object> taxonAttribute;
    private Attribute<Object> aliasesAttribute;
    private Attribute<Object> modificationDateAttribute;

    protected final Log log = LogFactory.getLog( GeneCacheImpl.class );

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#fetchBySymbols(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchBySymbols( Collection<String> geneSymbols ) {
        Criteria symbolCriteria = geneSymbolAttribute.in( geneSymbols );

        return fetchByCriteria( symbolCriteria );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#fetchByTaxons(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchByTaxons( Collection<Long> taxonIds ) {
        Criteria taxonCriteria = taxonAttribute.in( taxonIds );

        return fetchByCriteria( taxonCriteria );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#fetchById(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchByIds( Collection<Long> ids ) {
        Criteria idCriteria = idAttribute.in( ids );

        return fetchByCriteria( idCriteria );
    }

    @Override
    public Gene fetchById( Long id ) {
        Criteria idCriteria = idAttribute.eq( id );
        Collection<Gene> results = fetchByCriteria( idCriteria );
        if ( results.size() == 0 ) {
            return null;
        } else if ( results.size() > 1 ) {
            log.warn( "Multiple terms match GO ID: (" + id + "), return first hit" );
        }
        return fetchByCriteria( idCriteria ).iterator().next();
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
    public Collection<Gene> fetchByQuery( String queryString, Long taxonId ) {
        ArrayList<Gene> results = new ArrayList<>();
        Criteria taxonCriteria = taxonAttribute.eq( taxonId );

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

        // 5. anywhere in Gene name
        regexQueryString = "*" + queryString + "*";
        nameCriteria = geneNameAttribute.ilike( regexQueryString );
        tmp = fetchByCriteria( taxonCriteria.and( nameCriteria ) );
        tmp.removeAll( results );
        results.addAll( tmp );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#fetchBySymbolsAndTaxon(java.util.Collection, java.lang.Long)
     */
    @Override
    public Collection<Gene> fetchBySymbolsAndTaxon( Collection<String> geneSymbols, Long taxonId ) {
        Criteria symbolCriteria = geneSymbolAttribute.in( geneSymbols );

        Criteria taxonCriteria = taxonAttribute.eq( taxonId );

        return fetchByCriteria( taxonCriteria.and( symbolCriteria ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#clearAll()
     */
    @Override
    public void clearAll() {
        removeAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.cache.GeneCache#size()
     */
    @Override
    public long size() {
        return getSize();
    }

    @Override
    public String getCacheName() {
        return CACHE_NAME;
    }

    @Override
    public Object getKey( Gene gene ) {
        return gene.getId();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
        idAttribute = getSearchAttribute( GENE_ID_SEARCH_ATTRIBUTE_NAME );
        geneNameAttribute = getSearchAttribute( GENE_NAME_SEARCH_ATTRIBUTE_NAME );
        geneSymbolAttribute = getSearchAttribute( GENE_SYMBOL_SEARCH_ATTRIBUTE_NAME );
        taxonAttribute = getSearchAttribute( TAXON_SEARCH_ATTRIBUTE_NAME );
        aliasesAttribute = getSearchAttribute( ALIASES_SEARCH_ATTRIBUTE_NAME );
        modificationDateAttribute = getSearchAttribute( MODIFICATION_DATE_SEARCH_ATTRIBUTE_NAME );
    }

}
