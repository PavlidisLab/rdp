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

package ubc.pavlab.rdp.server.dao;

import java.util.Collection;

import ubc.pavlab.rdp.server.model.Gene;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
public interface GeneDao extends DaoBase<Gene> {

    public Collection<Gene> findByOfficialSymbol( final String officialSymbol );

    public Gene findByOfficialSymbolAndTaxon( String symbol, Long taxonId );

    public Gene findById( Long id );

    public Collection<Gene> findByTaxonId( final Long taxonId );

    public void updateGeneTable( String filePath );

    public void truncateGeneTable();

}
