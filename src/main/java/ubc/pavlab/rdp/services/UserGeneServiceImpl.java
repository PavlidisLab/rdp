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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;
import ubc.pavlab.rdp.repositories.TaxonRepository;
import ubc.pavlab.rdp.repositories.UserGeneRepository;
import ubc.pavlab.rdp.security.Permissions;
import ubc.pavlab.rdp.settings.ApplicationSettings;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ubc.pavlab.rdp.util.CollectionUtils.by;
import static ubc.pavlab.rdp.util.CollectionUtils.nullOrContainsAtLeastOne;

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
    private UserService userService;

    @Autowired
    private PermissionEvaluator permissionEvaluator;

    @Autowired
    private ApplicationSettings applicationSettings;

    @Override
    public Page<UserGene> findByUserEnabledTrueNoAuth( Pageable pageable ) {
        return userGeneRepository.findByUserEnabledTrue( pageable );
    }

    @Override
    public Page<UserGene> findByUserEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType privacyLevelType, Pageable pageable ) {
        return userGeneRepository.findByPrivacyLevelAndUserEnabledTrueAndUserProfilePrivacyLevel( privacyLevelType, pageable );
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public long countUniqueAssociations() {
        return userGeneRepository.countDistinctGeneByUserEnabledTrueAndTierIn( getManualTiers() );
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public long countAssociations() {
        return userGeneRepository.countByUserEnabledTrueAndTierIn( getManualTiers() );
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public Map<String, Long> researcherCountByTaxon() {
        return taxonRepository.findByActiveTrue().stream()
                .collect( Collectors.toMap( Taxon::getCommonName, userGeneRepository::countDistinctUserByUserEnabledTrueAndTaxon ) );
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public Map<Integer, Long> researcherCountByTaxonId() {
        return taxonRepository.findByActiveTrueOrderByOrdering().stream()
                .collect( Collectors.toMap( Taxon::getId, userGeneRepository::countDistinctUserByUserEnabledTrueAndTaxon ) );
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public long countUsersWithGenes() {
        return userGeneRepository.countDistinctUserByUserEnabledTrue();
    }

    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public long countUniqueAssociationsAllTiers() {
        return userGeneRepository.countDistinctGeneByUserEnabledTrueAndTierIn( applicationSettings.getEnabledTiers() );
    }

    /**
     * Count the number of unique associations to human genes that are either direct or indirect via orthology among all
     * tiers.
     * <p>
     * This is also known as the "human gene coverage".
     */
    @Cacheable(cacheNames = "ubc.pavlab.rdp.stats", key = "#root.methodName")
    @Override
    public long countUniqueAssociationsToHumanAllTiers() {
        Optional<Taxon> humanTaxon = taxonRepository.findById( 9606 );
        if ( humanTaxon.isPresent() ) {
            Collection<Integer> humanGenes = new HashSet<>( userGeneRepository.findAllDistinctGeneIdByTaxon( humanTaxon.get() ) );
            humanGenes.addAll( userGeneRepository.findDistinctOrthologGeneIdsByOrthologToTaxon( 9606 ) );
            return humanGenes.size();
        } else {
            return 0L;
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Override
    @PostFilter("hasPermission(filterObject, 'read')")
    public List<UserGene> handleGeneSearch( Gene gene, @Nullable Set<TierType> tiers, @Nullable Taxon orthologTaxon, @Nullable Set<ResearcherPosition> researcherPositions, @Nullable Collection<ResearcherCategory> researcherCategories, @Nullable Collection<OrganInfo> organs, @Nullable Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        Stream<UserGene> results = handleGeneSearchInternal( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organs, ontologyTermInfos ).stream();
        if ( applicationSettings.getPrivacy().isEnableAnonymizedSearchResults() ) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            results = results
                    // These must be excluded because anonymizeUserGene cannot receive a non-verified user.
                    // FIXME: Ideally, we would not fetch them altogether, but it's really cumbersome adjust all those
                    //        methods in the repository layer to exclude non-verified account.
                    .filter( ug -> ug.getUser().isEnabled() )
                    .map( ug -> permissionEvaluator.hasPermission( auth, ug, Permissions.READ ) ? ug : userService.anonymizeUserGene( ug ) );
        }
        return results
                .sorted( UserGene.getComparator() )
                .collect( Collectors.toList() ); // we need to preserve the search order
    }

    private Set<UserGene> handleGeneSearchInternal( Gene gene, @Nullable Set<TierType> tiers, @Nullable Taxon orthologTaxon, @Nullable Set<ResearcherPosition> researcherPositions, @Nullable Collection<ResearcherCategory> researcherCategories, @Nullable Collection<OrganInfo> organs, @Nullable Map<Ontology, Set<OntologyTermInfo>> ontologyTermInfos ) {
        Set<UserGene> uGenes = new LinkedHashSet<>();

        // do this once to save time in the inner loop
        final Set<String> organUberonIds;
        if ( organs != null ) {
            organUberonIds = organs.stream().map( Organ::getUberonId ).collect( Collectors.toSet() );
        } else {
            organUberonIds = null;
        }

        // inferred terms, grouped by ontology
        final Map<Ontology, Set<Integer>> ontologyTermInfoIds;
        if ( ontologyTermInfos != null ) {
            ontologyTermInfoIds = ontologyTermInfos.entrySet().stream()
                    .collect( Collectors.toMap( Map.Entry::getKey, e -> ontologyService.inferTermIds( e.getValue() ) ) );
        } else {
            ontologyTermInfoIds = null;
        }

        // ortholog relationship is not reflexive (i.e. a gene is not its own ortholog), but we still want to display
        // that gene first when ortholog search is performed in the same MO
        if ( orthologTaxon == null || gene.getTaxon().equals( orthologTaxon ) ) {
            uGenes.addAll( tiers == null ? userGeneRepository.findByGeneId( gene.getGeneId() ) : userGeneRepository.findByGeneIdAndTierIn( gene.getGeneId(), tiers ).stream()
                    .filter( ug -> researcherPositions == null || researcherPositions.contains( ug.getUser().getProfile().getResearcherPosition() ) )
                    .filter( ug -> nullOrContainsAtLeastOne( researcherCategories, () -> ug.getUser().getProfile().getResearcherCategories() ) )
                    .filter( ortholog -> nullOrContainsAtLeastOne( organUberonIds, () -> ortholog.getUser().getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                    .filter( by( UserGene::getUser, userService.hasOntologyTermIn( ontologyTermInfoIds ) ) )
                    .collect( Collectors.toSet() ) );
        }

        uGenes.addAll( handleOrthologSearchInternal( gene, tiers, orthologTaxon, researcherPositions, researcherCategories, organUberonIds, ontologyTermInfoIds ) );

        return uGenes;
    }

    private Set<UserGene> handleOrthologSearchInternal( Gene gene, @Nullable Set<TierType> tiers, @Nullable Taxon orthologTaxon, @Nullable Set<ResearcherPosition> researcherPositions, @Nullable Collection<ResearcherCategory> researcherCategories, @Nullable Collection<String> organUberonIds, @Nullable Map<Ontology, Set<Integer>> ontologyTermInfoIds ) {
        return ( orthologTaxon == null ? userGeneRepository.findOrthologsByGeneId( gene.getGeneId() ) : userGeneRepository.findOrthologsByGeneIdAndTaxon( gene.getGeneId(), orthologTaxon ) ).stream()
                .filter( ortholog -> tiers == null || tiers.contains( ortholog.getTier() ) )
                .filter( ug -> researcherPositions == null || researcherPositions.contains( ug.getUser().getProfile().getResearcherPosition() ) )
                .filter( ug -> nullOrContainsAtLeastOne( researcherCategories, () -> ug.getUser().getProfile().getResearcherCategories() ) )
                .filter( ortholog -> nullOrContainsAtLeastOne( organUberonIds, () -> ortholog.getUser().getUserOrgans().values().stream().map( UserOrgan::getUberonId ).collect( Collectors.toSet() ) ) )
                .filter( by( UserGene::getUser, userService.hasOntologyTermIn( ontologyTermInfoIds ) ) )
                .collect( Collectors.toSet() );
    }

    @Override
    @Transactional
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

    private Set<TierType> getManualTiers() {
        return applicationSettings.getEnabledTiers().stream().filter( TierType.MANUAL::contains ).collect( Collectors.toSet() );
    }
}
