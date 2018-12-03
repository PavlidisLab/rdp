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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import java.util.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("userGeneService")
public class UserGeneServiceImpl implements UserGeneService {

    @Autowired
    private TaxonRepository taxonRepository;

    @Autowired
    private UserGeneRepository userGeneRepository;

    @Autowired
    private UserService userService;

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUniqueAssociations() {
        return userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL_TIERS );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countAssociations() {
        return userGeneRepository.countByTierIn( TierType.MANUAL_TIERS );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Map<String, Integer> researcherCountByTaxon() {
        Map<String, Integer> countByTaxon = new HashMap<>();
        for ( Taxon taxon : taxonRepository.findByActiveTrue() ) {
            countByTaxon.put( taxon.getCommonName(), userGeneRepository.countDistinctUserByTaxon( taxon ) );
        }

        return countByTaxon;
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUsersWithGenes() {
        return userGeneRepository.countDistinctUser();
    }

    @Override
    public Collection<UserGene> findByGene( int geneId ) {
        return securityFilter( userGeneRepository.findByGeneId( geneId ) );
    }

    @Override
    public Collection<UserGene> findByGene( int geneId, TierType tier ) {
        return securityFilter( userGeneRepository.findByGeneIdAndTier( geneId, tier ) );
    }

    @Override
    public Collection<UserGene> findByGene( int geneId, Set<TierType> tiers ) {
        return securityFilter( userGeneRepository.findByGeneIdAndTierIn( geneId, tiers ) );
    }

    @Override
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon ) {
        return securityFilter( userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( symbol, taxon ) );
    }

    @Override
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, TierType tier ) {
        return securityFilter(
                userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( symbol, taxon, tier ) );
    }

    @Override
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers ) {
        return securityFilter(
                userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( symbol, taxon, tiers ) );
    }

    private Collection<UserGene> securityFilter( Collection<UserGene> userGenes ) {

        Collection<UserGene> filteredUserGenes = new LinkedList<>();
        for ( UserGene userGene : userGenes ) {
            if ( userService.checkCurrentUserCanSee( userGene ) ) {
                filteredUserGenes.add( userGene );
            } else {
                System.out.println( "Filtered out found gene " + userGene.getSymbol() );
            }
        }

        return filteredUserGenes;
    }

}
