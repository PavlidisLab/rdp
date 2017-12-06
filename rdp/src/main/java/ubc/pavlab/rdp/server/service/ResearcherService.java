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

package ubc.pavlab.rdp.server.service;

import org.json.JSONObject;
import org.springframework.security.access.annotation.Secured;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Publication;
import ubc.pavlab.rdp.server.model.Researcher;

import java.util.Collection;
import java.util.HashMap;

/**
 * TODO Document Me
 *
 * @author ptan
 * @version $Id$
 */
public interface ResearcherService {

    @Secured({"GROUP_USER"})
    Researcher create( final Researcher researcher );

    @Secured({"IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN"})
    Researcher createAsAdmin( final Researcher researcher );

    @Secured({"GROUP_USER"})
    void update( Researcher researcher );

    @Secured({"IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN"})
    void updateAsAdmin( Researcher researcher );

    @Secured({"GROUP_ADMIN", "AFTER_ACL_READ"})
    Researcher findByEmail( final String email );

    @Secured({"GROUP_ADMIN", "AFTER_ACL_READ"})
    Researcher findByUserName( final String username );

    @Secured({"GROUP_USER", "AFTER_ACL_READ"})
    Researcher loadCurrentResearcher();

    @Secured({"GROUP_ADMIN"})
    void delete( Researcher researcher );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> loadAll();

    @Secured({"GROUP_USER", "AFTER_ACL_READ"})
    Researcher thaw( Researcher researcher );

    @Secured({"GROUP_USER"})
    boolean addGenes( Researcher researcher, final HashMap<Gene, TierType> genes );

    @Secured({"GROUP_USER"})
    boolean removeGenes( Researcher researcher, final Collection<Gene> genes );

    /**
     * Removes all the associated genes and replace it with the new ones
     *
     * @param researcher
     * @param genes
     */
    @Secured({"GROUP_USER"})
    boolean updateGenes( Researcher researcher, final HashMap<Gene, TierType> genes );

    @Secured({"GROUP_USER"})
    boolean updateGenesByTaxon( Researcher researcher, Long taxonId, HashMap<Gene, TierType> genes );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> findByGene( Gene gene );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> findByGene( Gene gene, TierType tier );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> findByLikeSymbol( Long taxonId, String symbol );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> findByLikeSymbol( Long taxonId, String symbol, TierType tier );

    @Secured({"GROUP_ADMIN"})
    Collection<Researcher> findByLikeName( String nameLike );

    @Secured({"GROUP_USER"})
    JSONObject toJSON( Researcher r );

    @Secured({"GROUP_USER"})
    boolean updateGOTermsForTaxon( Researcher researcher, Collection<GeneOntologyTerm> goTerms, Long taxonId );

    @Secured({"GROUP_USER"})
    boolean clearGOTermsForTaxon( Researcher researcher, Long taxonId );

    @Secured({"GROUP_USER"})
    boolean AddGOTerms( Researcher researcher, Collection<GeneOntologyTerm> goTerms );

    @Secured({"GROUP_USER"})
    boolean removeGenesByTiersAndTaxon( Researcher researcher, Collection<TierType> tiers, Long taxonId );

    @Secured({"GROUP_USER"})
    boolean removeGenesByTiers( Researcher researcher, Collection<TierType> tier );

    @Secured({"IS_AUTHENTICATED_ANONYMOUSLY"})
    Long countResearchers();

    @Secured({"IS_AUTHENTICATED_ANONYMOUSLY"})
    Long countResearchersWithGenes();

    @Secured({"GROUP_USER"})
    boolean calculateGenes( Researcher researcher, Long taxonId );

    @Secured({"GROUP_USER"})
    void refreshOverlaps( Researcher researcher, Long taxonId );

    @Secured({"GROUP_USER"})
    boolean AddPublications( Researcher researcher, Collection<Publication> tions );

    @Secured({"GROUP_USER"})
    boolean updatePublications( Researcher researcher, Collection<Publication> tions );

}
