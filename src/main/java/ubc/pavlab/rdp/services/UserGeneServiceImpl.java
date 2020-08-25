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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    TierService tierService;

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUniqueAssociations() {
        return userGeneRepository.countDistinctGeneByTierIn( TierType.MANUAL );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countAssociations() {
        return userGeneRepository.countByTierIn( TierType.MANUAL );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Map<String, Integer> researcherCountByTaxon() {
        Map<String, Integer> countByTaxon = new HashMap<>();
        for ( Taxon taxon : taxonRepository.findByActiveTrueOrderByOrdering() ) {
            countByTaxon.put( taxon.getCommonName(), userGeneRepository.countDistinctUserByTaxon( taxon ) );
        }

        return countByTaxon;
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUsersWithGenes() {
        return userGeneRepository.countDistinctUser();
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUniqueAssociationsAllTiers() {
        return userGeneRepository.countDistinctGeneByTierIn( tierService.getEnabledTiers() );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUniqueAssociationsToHumanAllTiers() {
        /* This is also called the "human gene coverage" */

        Collection<Integer> humanGenes = new HashSet<Integer>();

        // Add orthologs mapped to humans
        // TODO
        // humanGenes.addAll( userGeneRepository.findHumanGenesForTarget(
        //         userGeneRepository.findDistinctGeneByTierIn( TierType.ANY )
        // ));

        // Add directly entered human genes
        humanGenes.addAll( userGeneRepository.findAllHumanGenes() );

        return humanGenes.size();
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByGeneId( int geneId ) {
        return userGeneRepository.findByGeneId( geneId );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByGeneIdAndTier( int geneId, TierType tier ) {
        return userGeneRepository.findByGeneIdAndTier( geneId, tier );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByGeneIdAndTierAndUserOrgansIn( int geneId, TierType tier, Collection<UserOrgan> organs ) {
        return userGeneRepository.findByGeneIdAndTierAndUserUserOrgansIn( geneId, tier, organs );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByGeneIdAndTierIn( int geneId, Set<TierType> tiers ) {
        return userGeneRepository.findByGeneIdAndTierIn( geneId, tiers );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByGeneIdAndTierInAndUserOrgansIn( int geneId, Set<TierType> tiers, Collection<UserOrgan> organs ) {
        return userGeneRepository.findByGeneIdAndTierInAndUserUserOrgansIn( geneId, tiers, organs );
    }

    @Override
    public Collection<UserGene> findByGeneIdWithoutSecurityFilter( int geneId ) {
        return userGeneRepository.findByGeneId( geneId );
    }

    @Override
    public Collection<UserGene> findByGeneIdAndTierWithoutSecurityFilter( int geneId, TierType tier ) {
        return userGeneRepository.findByGeneIdAndTier( geneId, tier );
    }

    @Override
    public Collection<UserGene> findByGeneIdAndTierAndUserOrgansInWithoutSecurityFilter( int geneId, TierType tier, Collection<UserOrgan> organs ) {
        return userGeneRepository.findByGeneIdAndTierAndUserUserOrgansIn( geneId, tier, organs );
    }

    @Override
    public Collection<UserGene> findByGeneIdAndTierInWithoutSecurityFilter( int geneId, Set<TierType> tiers ) {
        return userGeneRepository.findByGeneIdAndTierIn( geneId, tiers );
    }

    @Override
    public Collection<UserGene> findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Collection<UserOrgan> organs ) {
        return userGeneRepository.findByGeneIdAndTierInAndUserUserOrgansIn( geneId, tiers, organs );
    }

    @Override
    public UserGene findBySymbolAndTaxon( String symbol, Taxon taxon ) {
        return userGeneRepository.findBySymbolAndTaxon( symbol, taxon );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon ) {
        return userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxon( symbol, taxon );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, TierType tier ) {
        return userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTier( symbol, taxon, tier );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findByLikeSymbol( String symbol, Taxon taxon, Set<TierType> tiers ) {
        return userGeneRepository.findBySymbolContainingIgnoreCaseAndTaxonAndTierIn( symbol, taxon, tiers );
    }

    /**
     * Find all user genes which are ortholog to the provided gene.
     *
     * @param gene
     * @return
     */
    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findOrthologs( Gene gene, Set<TierType> tiers ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneId( userGene.getGeneId() ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .collect( Collectors.toSet() );
    }

    /**
     * Find all user genes within a given taxon which are ortholog to the provided gene.
     *
     * @param gene
     * @param orthologTaxon
     * @return
     */
    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findOrthologsWithTaxon( Gene gene, Set<TierType> tiers, Taxon orthologTaxon ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneIdAndTaxon( userGene.getGeneId(), orthologTaxon ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<UserGene> findOrthologsWithoutSecurityFilter( Gene gene, Set<TierType> tiers ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneId( userGene.getGeneId() ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<UserGene> findOrthologsWithTaxonWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<UserOrgan>> organs ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneIdAndTaxon( userGene.getGeneId(), orthologTaxon ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .collect( Collectors.toSet() );
    }

    private Map<UUID, Integer> userGeneHiddenIds = new ConcurrentHashMap<>();

    @Override
    public UUID getHiddenIdForUserGene( UserGene userGene ) {
        UUID hiddenId = UUID.randomUUID();
        userGeneHiddenIds.put( hiddenId, userGene.getId() );
        return hiddenId;
    }

    @Override
    @PostAuthorize("hasPermission(returnObject, 'read')")
    public UserGene findUserGeneByHiddenId( UUID hiddenUserGeneId ) {
        return userGeneRepository.findById( userGeneHiddenIds.get( hiddenUserGeneId ) );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Collection<UserGene> orthologs, Optional<Collection<UserOrgan>> organs ) {
        Collection<UserGene> uGenes = new LinkedHashSet<>();
        if ( organs.isPresent() ) {
            uGenes.addAll( findByGeneIdAndTierInAndUserOrgansIn( gene.getGeneId(), tiers, organs.get() ) );
        } else {
            uGenes.addAll( findByGeneIdAndTierIn( gene.getGeneId(), tiers ) );
        }
        uGenes.addAll( orthologs );
        return uGenes;
    }

    @Override
    public Collection<UserGene> handleGeneSearchWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Collection<UserGene> orthologs, Optional<Collection<UserOrgan>> organs ) {
        Collection<UserGene> uGenes = new LinkedHashSet<>();
        if ( organs.isPresent() ) {
            uGenes.addAll( findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( gene.getGeneId(), tiers, organs.get() ) );
        } else {
            uGenes.addAll( findByGeneIdAndTierInWithoutSecurityFilter( gene.getGeneId(), tiers ) );
        }
        uGenes.addAll( orthologs );
        return uGenes;
    }
}
