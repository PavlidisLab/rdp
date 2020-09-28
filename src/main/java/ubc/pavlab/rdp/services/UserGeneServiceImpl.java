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

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.containsAny;

/**
 * Created by mjacobson on 17/01/18.
 */
@CommonsLog
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
    public Collection<UserGene> findByGeneIdAndTierInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return userGeneRepository.findByGeneIdAndTierIn( geneId, tiers ).stream()
                .filter( ug -> researcherTypes.map( rt -> rt.contains( ug.getUser().getProfile().getResearcherCategory() ) ).orElse( true ) )
                .filter( ortholog -> userOrgans.map( o -> containsAny( o, ortholog.getUser().getUserOrgans().values() ) ).orElse( true ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<UserGene> findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( int geneId, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return userGeneRepository.findByGeneIdAndTierIn( geneId, tiers ).stream()
                .filter( ug -> researcherTypes.map( rt -> rt.contains( ug.getUser().getProfile().getResearcherCategory() ) ).orElse( true ) )
                .filter( ortholog -> userOrgans.map( o -> containsAny( o, ortholog.getUser().getUserOrgans().values() ) ).orElse( true ) )
                .collect( Collectors.toSet() );
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

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findOrthologsByGeneAndTierInAndUserOrgansIn( Gene gene, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return findOrthologsByGeneAndTierInAndUserOrgansInWithoutSecurityFilter( gene, tiers, researcherTypes, userOrgans );
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public Collection<UserGene> findOrthologsByGeneAndTierInAndTaxonAndUserOrgansIn( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return findOrthologsByGeneAndTierInAndTaxonAndUserOrgansInWithoutSecurityFilter( gene, tiers, orthologTaxon, researcherTypes, userOrgans );
    }

    @Override
    public Collection<UserGene> findOrthologsByGeneAndTierInAndUserOrgansInWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneId( userGene.getGeneId() ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .filter( ug -> researcherTypes.map( rt -> rt.contains( ug.getUser().getProfile().getResearcherCategory() ) ).orElse( true ) )
                .filter( ortholog -> userOrgans.map( o -> containsAny( o, ortholog.getUser().getUserOrgans().values() ) ).orElse( true ) )
                .collect( Collectors.toSet() );
    }

    @Override
    public Collection<UserGene> findOrthologsByGeneAndTierInAndTaxonAndUserOrgansInWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> userOrgans ) {
        return userGeneRepository.findByGeneId( gene.getGeneId() )
                .stream()
                .map( userGene -> userGeneRepository.findOrthologsByGeneIdAndTaxon( userGene.getGeneId(), orthologTaxon ) )
                .flatMap( orthologs -> orthologs.stream() )
                .filter( ortholog -> tiers.contains( ortholog.getTier() ) )
                .filter( ug -> researcherTypes.map( rt -> rt.contains( ug.getUser().getProfile().getResearcherCategory() ) ).orElse( true ) )
                .filter( ortholog -> userOrgans.map( o -> containsAny( o, ortholog.getUser().getUserOrgans().values() ) ).orElse( true ) )
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
    public Collection<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Optional<Taxon> orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs ) {
        return handleGeneSearchWithoutSecurityFilter( gene, tiers, orthologTaxon, researcherTypes, organs );
    }

    @Override
    public Collection<UserGene> handleGeneSearchWithoutSecurityFilter( Gene gene, Set<TierType> tiers, Optional<Taxon> orthologTaxon, Optional<Collection<ResearcherCategory>> researcherTypes, Optional<Collection<UserOrgan>> organs ) {
        Collection<UserGene> uGenes = new LinkedHashSet<>();

        // ortholog relationship is not reflexive (i.e. a gene is not its own ortholog), but we still want to display
        // that gene first when ortholog search is performed in the same MO
        if ( gene.getTaxon().equals( orthologTaxon.orElse( gene.getTaxon() ) ) ) {
            if ( organs.isPresent() ) {
                uGenes.addAll( findByGeneIdAndTierInAndUserOrgansInWithoutSecurityFilter( gene.getGeneId(), tiers, researcherTypes, organs ) );
            } else {
                uGenes.addAll( findByGeneIdAndTierInWithoutSecurityFilter( gene.getGeneId(), tiers, researcherTypes, organs ) );
            }
        }

        if ( orthologTaxon.isPresent() ) {
            uGenes.addAll( findOrthologsByGeneAndTierInAndTaxonAndUserOrgansInWithoutSecurityFilter( gene, tiers, orthologTaxon.get(), researcherTypes, organs ) );
        } else {
            uGenes.addAll( findOrthologsByGeneAndTierInAndUserOrgansInWithoutSecurityFilter( gene, tiers, researcherTypes, organs ) );
        }

        return uGenes;
    }

    @Override
    public void updateUserGenes() {
        log.info( "Updating user genes..." );
        for ( UserGene userGene : userGeneRepository.findAllWithGeneInfo() ) {
            GeneInfo cachedGene = userGene.getGeneInfo();
            if ( cachedGene == null ) {
                log.warn( MessageFormat.format( "User has a reference to a gene missing from the cache: {0}.", userGene ) );
                continue;
            }
            userGene.updateGene( cachedGene );
            userGeneRepository.save( userGene );
        }
        log.info( "Done updating user genes." );
    }
}
