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

import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;

import java.util.*;

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

    Collection<UserGene> findByGeneIdAndTierInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs );

    Collection<UserGene> findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs );

    UserGene findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, TierType tier );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers );

    Collection<UserGene> findOrthologsByGeneAndTierInAndUserOrgansIn( Gene gene, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans );

    Collection<UserGene> findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans );

    Collection<UserGene> findOrthologsByGeneAndTierInAndUserOrgansInWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans );

    Collection<UserGene> findOrthologsByGeneAndTierInAndTaxonAndUserOrgansInWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans );

    UUID getHiddenIdForUserGene( UserGene userGene );

    UserGene findUserGeneByHiddenId( UUID hiddenUserGeneId );

    Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Optional<Taxon> orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs );

    Collection<UserGene> handleGeneSearchWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Optional<Taxon> orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs );

    void updateUserGenes();
}
