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

import java.util.Collection;

import ubc.pavlab.rdp.server.model.Gene;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface GeneCache {

    public boolean hasExpired();

    public Collection<Gene> fetchBySymbols( Collection<String> geneSymbols );

    public Collection<Gene> fetchByTaxons( Collection<Long> taxonIds );

    public Collection<Gene> fetchByIds( Collection<Long> ids );

    public Gene fetchById( Long id );

    public Collection<Gene> fetchByQuery( String queryString, Long taxonId );

    public Collection<Gene> fetchBySymbolsAndTaxon( Collection<String> geneSymbols, Long taxonId );

    public void putAll( Collection<Gene> genes );

    public void clearAll();

    public long size();

}
