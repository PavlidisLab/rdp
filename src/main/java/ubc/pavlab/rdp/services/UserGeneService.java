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

import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface UserGeneService {

    Integer countUniqueAssociations();
    Integer countAssociations();
    Map<String, Integer> researcherCountByTaxon();

    Integer countUsersWithGenes();

    Collection<UserGene> findByGene( int geneId );

    Collection<UserGene> findByGene( int geneId, TierType tier );

    Collection<UserGene> findByGene( int geneId, Set<TierType> tiers );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, TierType tier );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers );
}
