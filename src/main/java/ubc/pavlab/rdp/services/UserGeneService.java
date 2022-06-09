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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ubc.pavlab.rdp.model.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.OntologyTermInfo;

import java.util.*;

/**
 * Created by mjacobson on 17/01/18.
 */
public interface UserGeneService {

    Page<UserGene> findByUserEnabledTrueNoAuth( Pageable pageable );

    Page<UserGene> findByUserEnabledTrueAndPrivacyLevelNoAuth( PrivacyLevelType privacyLevelType, Pageable pageable );

    Integer countUniqueAssociations();

    Integer countAssociations();

    Map<String, Integer> researcherCountByTaxon();

    Integer countUsersWithGenes();

    Integer countUniqueAssociationsAllTiers();

    Integer countUniqueAssociationsToHumanAllTiers();

    /**
     * Perform a search and retrieve all the user genes that match the provided gene description and optional
     * constraints.
     * <p>
     * Results are sorted by taxon (according to {@link Taxon#getOrdering()}), tier type, researcher last name and
     * first name. Anonymous results are always displayed last.
     *
     * @param tiers               only retain results in the given {@link TierType}, or any if null
     * @param orthologTaxon       only retain results in the given ortholog {@link Taxon}, or any if null
     * @param researcherPositions only retain results where the corresponding {@link User} holds any given {@link ResearcherPosition},
     *                            or any if null
     * @param researcherTypes     only retain results where the corresponding {@link User} has any of the given {@link ResearcherCategory}
     *                            or any if null
     * @param organs              only retain results where the corresponding {@link User} tracks any of the given {@link OrganInfo}
     * @param ontologyTermInfos
     */
    List<UserGene> handleGeneSearch( Gene gene, Set<TierType> tiers, Taxon orthologTaxon, Set<ResearcherPosition> researcherPositions, Collection<ResearcherCategory> researcherTypes, Collection<OrganInfo> organs, Collection<OntologyTermInfo> ontologyTermInfos );

    void updateUserGenes();
}
