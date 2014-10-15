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
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.cache.GeneCache;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Taxon;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service
public class GeneCacheServiceImpl implements GeneCacheService {

    private static Log log = LogFactory.getLog( GeneCacheServiceImpl.class.getName() );

    private static AtomicBoolean updatingCache = new AtomicBoolean( false );

    @Autowired
    private GeneCache geneCache;

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxonService taxonService;

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#fetchBySymbols(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchBySymbols( Collection<String> geneSymbols ) {
        return geneCache.fetchBySymbols( geneSymbols );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#fetchByTaxons(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchByTaxons( Collection<Long> taxonIds ) {
        return geneCache.fetchByTaxons( taxonIds );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#fetchById(java.util.Collection)
     */
    @Override
    public Collection<Gene> fetchById( Collection<Long> ids ) {
        return geneCache.fetchById( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#fetchByQuery(java.lang.String, java.lang.Long)
     */
    @Override
    public Collection<Gene> fetchByQuery( String queryString, Long taxonId ) {
        return geneCache.fetchByQuery( queryString, taxonId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#fetchBySymbolsAndTaxon(java.util.Collection, java.lang.Long)
     */
    @Override
    public Collection<Gene> fetchBySymbolsAndTaxon( Collection<String> geneSymbols, Long taxonId ) {
        return geneCache.fetchBySymbolsAndTaxon( geneSymbols, taxonId );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#updateCache()
     */
    @Override
    public long updateCache() {
        long cacheSize = -1;
        if ( updatingCache.compareAndSet( false, true ) ) {
            try {
                Collection<Taxon> taxons = taxonService.loadAll();
                for ( Taxon taxon : taxons ) {
                    Long taxonId = taxon.getId();
                    Collection<Gene> genes = geneService.findByTaxonId( taxonId );
                    if ( genes != null ) {
                        log.info( "Caching a total of " + genes.size() + " genes for taxon: " + taxon.getCommonName() );
                        this.geneCache.putAll( genes );
                    }
                }

                cacheSize = this.geneCache.size();
                log.info( "Current size of Cache: " + cacheSize );
            } finally {
                updatingCache.set( false );
            }
        } else {
            String errorMessage = "Update Cache already running!";
            log.error( errorMessage );
            throw new IllegalThreadStateException( errorMessage );
        }

        return cacheSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubc.pavlab.rdp.server.service.GeneCacheService#clearCache()
     */
    @Override
    public void clearCache() {
        geneCache.clearAll();
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() {
    }

}
