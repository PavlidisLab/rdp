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

import gemma.gsec.model.UserGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.dao.UserDao;
import ubc.pavlab.rdp.server.dao.UserGroupDao;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;
import ubc.pavlab.rdp.server.model.Publication;
import ubc.pavlab.rdp.server.model.Researcher;
import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
@Service("researcherService")
public class ResearcherServiceImpl implements ResearcherService {

    private static Log log = LogFactory.getLog( ResearcherServiceImpl.class );

    @Autowired
    ResearcherDao researcherDao;

    @Autowired
    UserDao userDao;

    @Autowired
    UserGroupDao userGroupDao;

    @Autowired
    GeneService geneService;

    // @Autowired
    // GeneOntologyService geneOntologyService;

    @Autowired
    GOService gOService;

    @Autowired
    GeneDao geneDao;

    @Override
    @Transactional
    public Researcher create( final Researcher researcher ) {
        return researcherDao.create( researcher );
    }

    @Override
    @Transactional
    public Researcher createAsAdmin( final Researcher researcher ) {
        return researcherDao.create( researcher );
    }

    @Override
    @Transactional
    public void update( Researcher researcher ) {
        researcherDao.update( researcher );
    }

    @Override
    @Transactional
    public void updateAsAdmin( Researcher researcher ) {
        researcherDao.update( researcher );
    }

    @Override
    public Researcher findByEmail( final String email ) {
        return researcherDao.findByEmail( email );
    }

    @Override
    public Researcher findByUserName( final String username ) {
        return researcherDao.findByUsername( username );
    }

    @Override
    public void delete( Researcher researcher ) {

        /**
         * We can only delete Researcher.Contact if it's no longer referenced in UserGroup!
         */
        if ( researcher != null ) {
            User contact = researcher.getContact();
            for ( UserGroup group : this.userDao.loadGroups( contact ) ) {
                group.getGroupMembers().remove( contact );
                this.userGroupDao.update( ( ubc.pavlab.rdp.server.model.common.auditAndSecurity.UserGroup ) group );
            }

            researcherDao.remove( researcher );
        }
    }

    @Override
    public Researcher thaw( Researcher researcher ) {
        return researcherDao.thaw( researcher );
    }

    @Override
    public Collection<Researcher> loadAll() {
        return ( Collection<Researcher> ) researcherDao.loadAll();
    }

    @Override
    @Transactional
    public boolean addGenes( Researcher researcher, final HashMap<Gene, TierType> genes ) {

        int numGenesBefore = researcher.getGenes().size();
        boolean modified = false;

        for ( Entry<Gene, TierType> entry : genes.entrySet() ) {
            Gene gene = entry.getKey();
            TierType tier = entry.getValue();
            if ( gene.getId() == null ) {
                log.warn( "Attempting to add gene without ID to researcher: " + researcher );
                // geneDao.create( gene );
            } else {
                GeneAssociation ga = researcher.getGeneAssociatonFromGene( gene );
                if ( ga != null ) {
                    log.warn( "gene already exists :" + gene );
                    // gene already added to researcher
                    modified |= researcher.removeGeneAssociation( ga );
                }
                modified |= researcher.addGeneAssociation( new GeneAssociation( gene, researcher, tier ) );
            }
        }

        if ( modified ) {
            researcherDao.update( researcher );
            log.info( "Added " + ( researcher.getGenes().size() - numGenesBefore ) + " genes to Researcher "
                    + researcher );
        }

        return modified;
    }

    @Override
    @Transactional
    public boolean removeGenes( Researcher researcher, Collection<Gene> genes ) {

        int numGenesBefore = researcher.getGenes().size();
        boolean modified = false;
        Collection<GeneAssociation> geneAssociations = researcher.getGeneAssociatonsFromGenes( genes );
        for ( GeneAssociation geneAssociation : geneAssociations ) {
            Gene gene = geneAssociation.getGene();
            if ( gene.getId() != null ) {

                modified = researcher.removeGeneAssociation( geneAssociation );
            } else {
                // FIXME has to search for the GeneAssocation?
                Gene matchedGene = geneDao.findById( gene.getId() );
                if ( matchedGene != null ) {
                    modified = researcher.removeGeneAssociation( geneAssociation );
                } else {
                    log.warn( "Gene " + gene + " was not removed from Researcher " + researcher
                            + " because it was found in the database" );
                }
            }
        }

        researcherDao.update( researcher );

        log.info( "Removed " + ( numGenesBefore - researcher.getGenes().size() ) + " genes to Researcher " + researcher );

        return modified;
    }

    @Override
    @Transactional
    public boolean updateGenes( Researcher researcher, HashMap<Gene, TierType> genes ) {
        Collection<TierType> tiersToRemove = new HashSet<TierType>();
        tiersToRemove.add( TierType.TIER1 );
        tiersToRemove.add( TierType.TIER2 );
        this.removeGenesByTiers( researcher, tiersToRemove );
        // researcher.getGeneAssociations().clear();
        boolean added = addGenes( researcher, genes );
        researcherDao.update( researcher );
        return added;
    }

    @Override
    @Transactional
    public boolean updateGenesByTaxon( Researcher researcher, Long taxonId, HashMap<Gene, TierType> genes ) {
        Collection<TierType> tiersToRemove = new HashSet<TierType>();
        tiersToRemove.add( TierType.TIER1 );
        tiersToRemove.add( TierType.TIER2 );
        boolean modified = this.removeGenesByTiersAndTaxon( researcher, tiersToRemove, taxonId );
        // researcher.getGeneAssociations().clear();
        modified |= addGenes( researcher, genes );
        if ( modified ) {
            researcherDao.update( researcher );
        }
        return modified;
    }

    @Override
    @Transactional
    public boolean removeGenesByTiersAndTaxon( Researcher researcher, Collection<TierType> tiers, Long taxonId ) {
        boolean modified = false;
        for ( Iterator<GeneAssociation> i = researcher.getGeneAssociations().iterator(); i.hasNext(); ) {
            GeneAssociation ga = i.next();
            if ( tiers.contains( ga.getTier() ) && ga.getGene().getTaxonId().equals( taxonId ) ) {
                i.remove();
                modified = true;
            }
        }

        return modified;
    }

    @Override
    @Transactional
    public boolean removeGenesByTiers( Researcher researcher, Collection<TierType> tiers ) {
        boolean modified = false;
        for ( Iterator<GeneAssociation> i = researcher.getGeneAssociations().iterator(); i.hasNext(); ) {
            GeneAssociation ga = i.next();
            if ( tiers.contains( ga.getTier() ) ) {
                i.remove();
                modified = true;
            }
        }

        if ( modified ) {
            researcherDao.update( researcher );
        }

        return modified;
    }

    @Override
    @Transactional
    public boolean updateGOTermsForTaxon( Researcher researcher, Collection<GeneOntologyTerm> goTerms, Long taxonId ) {
        boolean modified = false;
        modified = clearGOTermsForTaxon( researcher, taxonId );
        modified |= AddGOTerms( researcher, goTerms );

        if ( modified ) {
            researcherDao.update( researcher );
        }

        return modified;
    }

    @Override
    @Transactional
    public boolean clearGOTermsForTaxon( Researcher researcher, Long taxonId ) {
        boolean modified = false;
        for ( Iterator<GeneOntologyTerm> i = researcher.getGoTerms().iterator(); i.hasNext(); ) {
            GeneOntologyTerm term = i.next();
            if ( term.getTaxonId().equals( taxonId ) ) {
                i.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    @Transactional
    public boolean AddGOTerms( Researcher researcher, Collection<GeneOntologyTerm> goTerms ) {
        boolean modified = false;
        for ( GeneOntologyTerm term : goTerms ) {
            modified |= researcher.addGOTerm( term );
        }
        return modified;
    }

    @Override
    @Transactional
    public boolean updatePublications( Researcher researcher, Collection<Publication> publications ) {
        researcher.getPublications().clear();
        boolean added = AddPublications( researcher, publications );
        researcherDao.update( researcher );
        return added;
    }

    @Override
    @Transactional
    public boolean AddPublications( Researcher researcher, Collection<Publication> publications ) {
        boolean modified = false;
        for ( Publication pub : publications ) {
            modified |= researcher.addPublication( pub );
        }
        return modified;
    }

    @Override
    @Transactional
    public boolean calculateGenes( Researcher researcher, Long taxonId ) {
        HashMap<Gene, TierType> calculatedGenes = new HashMap<Gene, TierType>();

        Collection<TierType> tiersToRemove = new HashSet<TierType>();
        tiersToRemove.add( TierType.TIER3 );
        boolean modified = removeGenesByTiersAndTaxon( researcher, tiersToRemove, taxonId );

        for ( Gene g : gOService.getRelatedGenes( researcher.getGoTerms(), taxonId ) ) {
            calculatedGenes.put( g, TierType.TIER3 );
        }

        calculatedGenes.keySet().removeAll( researcher.getGenesByTaxonId( taxonId ) );

        modified |= addGenes( researcher, calculatedGenes );

        if ( modified ) {
            researcherDao.update( researcher );
        }

        return modified;
    }

    @Override
    @Transactional
    public void refreshOverlaps( Researcher researcher, Long taxonId ) {
        Set<GeneOntologyTerm> goTerms = researcher.getGoTermsByTaxonId( taxonId );
        Collection<Gene> genes = researcher.getDirectGenesInTaxon( taxonId );
        for ( GeneOntologyTerm term : goTerms ) {
            term.setFrequency( gOService.computeOverlapFrequency( term.getGeneOntologyId(), genes ) );
        }

        researcherDao.update( researcher );
    }

    @Override
    public Collection<Researcher> findByGene( Gene gene ) {
        return researcherDao.findByGene( gene );
    }

    @Override
    public Collection<Researcher> findByGene( Gene gene, TierType tier ) {
        return researcherDao.findByGene( gene, tier );
    }

    @Override
    public Collection<Researcher> findByLikeSymbol( String symbol ) {
        return researcherDao.findByLikeSymbol( symbol );
    }

    @Override
    public Collection<Researcher> findByLikeSymbol( String symbol, TierType tier ) {
        return researcherDao.findByLikeSymbol( symbol, tier );
    }

    @Override
    public Long countResearchers() {
        return researcherDao.countResearchers();
    }

    @Override
    public Long countResearchersWithGenes() {
        return researcherDao.countResearchersWithGenes();
    }

    @Override
    public JSONObject toJSON( Researcher r ) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put( "contact", r.getContact().toJSON() );
        jsonObj.put( "department", r.getDepartment() );
        jsonObj.put( "description", r.getDescription() );
        jsonObj.put( "organization", r.getOrganization() );
        jsonObj.put( "phone", r.getPhone() );
        jsonObj.put( "website", r.getWebsite() );
        jsonObj.put( "description", r.getDescription() );
        jsonObj.put( "publications", r.getPublications() );

        jsonObj.put( "taxonDescriptions", r.getTaxonDescriptions() );

        jsonObj.put( "genes", geneService.toJSON( r.getGeneAssociations() ) );

        jsonObj.put( "terms", gOService.toJSON( r.getGoTerms() ) );

        return jsonObj;
    }

}
