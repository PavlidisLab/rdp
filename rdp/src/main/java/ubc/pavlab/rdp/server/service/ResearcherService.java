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

import java.util.Collection;
import java.util.HashMap;

import org.json.JSONObject;
import org.springframework.security.access.annotation.Secured;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Publication;
import ubc.pavlab.rdp.server.model.Researcher;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
public interface ResearcherService {

    @Secured({ "GROUP_USER" })
    public Researcher create( final Researcher researcher );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    public Researcher createAsAdmin( final Researcher researcher );

    @Secured({ "GROUP_USER" })
    public void update( Researcher researcher );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    public void updateAsAdmin( Researcher researcher );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Researcher findByEmail( final String email );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Researcher findByUserName( final String username );

    @Secured({ "GROUP_ADMIN" })
    public void delete( Researcher researcher );

    @Secured({ "GROUP_ADMIN" })
    public Collection<Researcher> loadAll();

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Researcher thaw( Researcher researcher );

    @Secured({ "GROUP_USER" })
    public boolean addGenes( Researcher researcher, final HashMap<Gene, TierType> genes );

    @Secured({ "GROUP_USER" })
    public boolean removeGenes( Researcher researcher, final Collection<Gene> genes );

    /**
     * Removes all the associated genes and replace it with the new ones
     * 
     * @param researcher
     * @param genes
     */
    @Secured({ "GROUP_USER" })
    public boolean updateGenes( Researcher researcher, final HashMap<Gene, TierType> genes );

    @Secured({ "GROUP_USER" })
    public boolean updateGenesByTaxon( Researcher researcher, Long taxonId, HashMap<Gene, TierType> genes );

    @Secured({ "GROUP_USER" })
    public Collection<Researcher> findByGene( Gene gene );

    @Secured({ "GROUP_USER" })
    public Collection<Researcher> findByGene( Gene gene, TierType tier );

    @Secured({ "GROUP_USER" })
    public Collection<Researcher> findByLikeSymbol( Long taxonId, String symbol );

    @Secured({ "GROUP_USER" })
    public Collection<Researcher> findByLikeSymbol( Long taxonId, String symbol, TierType tier );

    @Secured({ "GROUP_USER" })
    public Collection<Researcher> findByLikeName( String nameLike );

    @Secured({ "GROUP_USER" })
    public JSONObject toJSON( Researcher r );

    @Secured({ "GROUP_USER" })
    public boolean updateGOTermsForTaxon( Researcher researcher, Collection<GeneOntologyTerm> goTerms, Long taxonId );

    @Secured({ "GROUP_USER" })
    public boolean clearGOTermsForTaxon( Researcher researcher, Long taxonId );

    @Secured({ "GROUP_USER" })
    public boolean AddGOTerms( Researcher researcher, Collection<GeneOntologyTerm> goTerms );

    @Secured({ "GROUP_USER" })
    public boolean removeGenesByTiersAndTaxon( Researcher researcher, Collection<TierType> tiers, Long taxonId );

    @Secured({ "GROUP_USER" })
    public boolean removeGenesByTiers( Researcher researcher, Collection<TierType> tier );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Long countResearchers();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Long countResearchersWithGenes();

    @Secured({ "GROUP_USER" })
    public boolean calculateGenes( Researcher researcher, Long taxonId );

    @Secured({ "GROUP_USER" })
    public void refreshOverlaps( Researcher researcher, Long taxonId );

    @Secured({ "GROUP_USER" })
    public boolean AddPublications( Researcher researcher, Collection<Publication> publications );

    @Secured({ "GROUP_USER" })
    public boolean updatePublications( Researcher researcher, Collection<Publication> publications );

}
