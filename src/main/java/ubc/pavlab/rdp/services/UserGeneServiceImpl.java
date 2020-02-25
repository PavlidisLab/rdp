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
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserGene;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("userGeneService")
public class UserGeneServiceImpl implements UserGeneService {

    private static final Integer ALL_TAXON_ID = -99;
    @Autowired
    private TaxonRepository taxonRepository;

    @Autowired
    private UserGeneRepository userGeneRepository;

    @Autowired
    private GeneService geneService;

    @Autowired
    private UserService userService;

    @Autowired
    private PrivacyService privacyService;

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
        return userGeneRepository.countDistinctGeneByTierIn( TierType.ALL_TIERS   );
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Integer countUniqueAssociationsToHumanAllTiers() {
	/* This is also called the "human gene coverage" */

        Collection<Integer> humanGenes = new HashSet<Integer>();

        // Add orthologs mapped to humans
        humanGenes.addAll( userGeneRepository.findHumanGenesForTarget(
                userGeneRepository.findDistinctGeneByTierIn( TierType.ALL_TIERS   )
        ));

        // Add directly entered human genes
        humanGenes.addAll( userGeneRepository.findAllHumanGenes() );

        return humanGenes.size();
    }

    @Cacheable(cacheNames = "stats", key = "#root.methodName")
    @Override
    public Collection<Integer> findOrthologs( Integer sourceGene, Integer targetTaxon ) {
        return userGeneRepository.findOrthologs( sourceGene, targetTaxon );
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

    @Override
    public Collection<Gene> findOrthologs( Gene gene, Integer orthologTaxonId ) {
        if ( gene == null || orthologTaxonId == null ) {
            //noinspection unchecked
            return Collections.EMPTY_LIST;
        }
        if ( ALL_TAXON_ID.equals( orthologTaxonId ) ) { // Looking for all taxa
            Collection<Gene> genes = new LinkedList<>();
            for ( Taxon taxon : taxonRepository.findAll() ) {
                Collection<Gene> taxonGenes = findOrthologsForTaxon( gene, taxon.getId() );
                genes.addAll(taxonGenes.stream().filter(Objects::nonNull).collect( Collectors.toList()) );
            }
            genes.add( gene ); // Add original gene so it shows up in the results as well.
            return genes;
        } else { // Only looking for one taxon
            if ( taxonRepository.findOne( orthologTaxonId ) == null ) {
                //noinspection unchecked
                return Collections.EMPTY_LIST;
            }
            return findOrthologsForTaxon( gene, orthologTaxonId );
        }

    }

    private Collection<UserGene> securityFilter( Collection<UserGene> userGenes ) {
        return userGenes.stream().filter(privacyService::checkCurrentUserCanSee).collect(Collectors.toList());
    }

    private Collection<Gene> findOrthologsForTaxon( Gene gene, Integer targetTaxonId ) {
        Collection<Integer> geneIds = findOrthologs( gene.getGeneId(), targetTaxonId );
        if ( geneIds == null || geneIds.isEmpty() ) {
            //noinspection unchecked
            return Collections.EMPTY_LIST;
        }
        return geneService.load( geneIds );
    }

}
