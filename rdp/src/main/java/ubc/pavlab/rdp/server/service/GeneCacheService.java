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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.access.annotation.Secured;

import ubc.pavlab.rdp.server.model.Gene;

/**
 * Used to manipulate and load GeneCache
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface GeneCacheService extends InitializingBean {

    @Secured({ "GROUP_USER" })
    public Collection<Gene> fetchBySymbols( Collection<String> geneSymbols );

    @Secured({ "GROUP_USER" })
    public Collection<Gene> fetchByTaxons( Collection<Long> taxonIds );

    @Secured({ "GROUP_USER" })
    public Gene fetchById( Long id );

    @Secured({ "GROUP_USER" })
    public Collection<Gene> fetchByIds( Collection<Long> ids );

    @Secured({ "GROUP_USER" })
    public Collection<Gene> fetchByQuery( String queryString, Long taxonId );

    @Secured({ "GROUP_USER" })
    public Collection<Gene> fetchBySymbolsAndTaxon( Collection<String> geneSymbols, Long taxonId );

    @Secured({ "GROUP_ADMIN" })
    public long updateCache();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    public long loadCache();

    @Secured({ "GROUP_ADMIN" })
    public void clearCache();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    public abstract void init( boolean force );

}
