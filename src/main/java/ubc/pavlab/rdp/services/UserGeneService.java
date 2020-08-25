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

import org.springframework.security.access.prepost.PostFilter;
import ubc.pavlab.rdp.model.*;
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

    Collection<UserGene> findByGeneId( int geneId );

    Collection<UserGene> findByGeneIdAndTier( int geneId, TierType tier );

    @PostFilter("hasPermission(filterObject, 'read')")
    Collection<UserGene> findByGeneIdAndTierAndUserOrgansIn( int geneId, TierType tier, Collection<UserOrgan> organs );

    Collection<UserGene> findByGeneIdAndTierIn( int geneId, Set<TierType> tiers );

    Collection<UserGene> findByGeneIdAndTierInAndUserOrgansIn( int geneId, Set<TierType> tiers, Collection<UserOrgan> organs );

    Collection<UserGene> findByGeneIdWithoutSecurityFilter( int geneId );

    Collection<UserGene> findByGeneIdAndTierWithoutSecurityFilter( int geneId, TierType tier );

    Collection<UserGene> findByGeneIdAndTierAndUserOrgansInWithoutSecurityFilter( int geneId, TierType tier, Collection<UserOrgan> organs );

    Collection<UserGene> findByGeneIdAndTierInWithoutSecurityFilter( int geneId, Set<TierType> tiers );

    Collection<UserGene> findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Collection<UserOrgan> organs );

    UserGene findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, TierType tier );

    Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers );

    Collection<UserGene> findOrthologs( Gene gene, Set<TierType> tiers );

    Collection<UserGene> findOrthologsWithTaxon( Gene gene, Set<TierType> tiers, Taxon orthologTaxon );

    Collection<UserGene> findOrthologsWithoutSecurityFilter( Gene gene, Set<TierType> tiers );

    Collection<UserGene> findOrthologsWithTaxonWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<UserOrgan>> organs );

    UUID getHiddenIdForUserGene( UserGene userGene );

    UserGene findUserGeneByHiddenId( UUID hiddenUserGeneId );

    Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Collection<UserGene> orthologs, Optional<Collection<UserOrgan>> organs );

    Collection<UserGene> handleGeneSearchWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Collection<UserGene> orthologs, Optional<Collection<UserOrgan>> organs );
}
