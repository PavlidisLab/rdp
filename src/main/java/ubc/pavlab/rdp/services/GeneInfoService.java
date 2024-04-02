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

package ubc.pavlab.rdp.services;

import org.springframework.lang.Nullable;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.util.SearchResult;

import java.util.Collection;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface GeneInfoService {

    @Nullable
    GeneInfo load( Integer geneId );

    Collection<GeneInfo> load( Collection<Integer> ids );

    @Nullable
    GeneInfo findBySymbolAndTaxon( String officialSymbol, Taxon taxon );

    Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    Collection<SearchResult<GeneInfo>> autocomplete( String query, Taxon taxon, int maxResults );

    /**
     * Update gene informations in the database.
     */
    void updateGenes();

    /**
     * Update gene orthologs in the database.
     */
    void updateGeneOrthologs();
}
