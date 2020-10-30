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

import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.UserOrgan;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
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

    Integer countUniqueAssociationsAllTiers();

    Integer countUniqueAssociationsToHumanAllTiers();

    Collection<UserGene> findByGeneIdAndTierIn( int geneId, Set<TierType> tiers, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes );

    Collection<UserGene> findByGeneIdAndTierInAndUserOrgansIn( int geneId, Set<TierType> tiers, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<UserGene> findOrthologsByGeneAndTierInAndUserOrgansIn( Gene gene, Set<TierType> tiers, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<UserGene> findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> userOrgans );

    Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<UserOrgan> organs );

    void updateUserGenes();
}
