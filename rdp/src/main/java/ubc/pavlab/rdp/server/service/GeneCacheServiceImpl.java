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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.cache.GeneCache;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Service
public class GeneCacheServiceImpl implements GeneCacheService {

    private static Log log = LogFactory.getLog( GeneCacheServiceImpl.class.getName() );

    private static final AtomicBoolean updatingCache = new AtomicBoolean( false );

    private static final AtomicBoolean running = new AtomicBoolean( false );

    private static final AtomicBoolean ready = new AtomicBoolean( false );

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
    public Collection<Gene> fetchByIds( Collection<Long> ids ) {
        return geneCache.fetchByIds( ids );
    }

    @Override
    public Gene fetchById( Long id ) {
        return geneCache.fetchById( id );
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
        return loadCache();
    }

    @Override
    public long loadCache() {
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

    @Override
    public void afterPropertiesSet() {
        this.init( false );
    }

    @Override
    public synchronized void init( boolean force ) {

        if ( running.get() ) {
            log.warn( "Gene Cache initialization is already running" );
            return;
        }

        initializeGeneCache();
    }

    private synchronized void initializeGeneCache() {
        if ( running.get() ) return;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                running.set( true );
                log.info( "Loading Gene Cache..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {

                    // Must attach a user with authorities to the session so that we can use injected beans
                    User user = new User();
                    List<GrantedAuthority> dbAuths = new ArrayList<GrantedAuthority>();
                    dbAuths.add( new SimpleGrantedAuthority( "ROLE_ADMIN" ) );
                    Authentication auth = new UsernamePasswordAuthenticationToken( user, null, dbAuths );
                    SecurityContextHolder.getContext().setAuthentication( auth );

                    long count = loadCache();
                    log.info( "Gene Cache loaded, total of " + count + " items in " + loadTime.getTime() / 1000 + "s" );
                    ready.set( true );
                    running.set( false );

                    loadTime.stop();
                } catch ( Throwable e ) {
                    if ( log != null ) log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        loadThread.start();

    }

}
