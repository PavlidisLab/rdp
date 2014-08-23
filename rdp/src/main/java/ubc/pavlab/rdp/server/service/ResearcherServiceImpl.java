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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubc.pavlab.rdp.server.dao.GeneDao;
import ubc.pavlab.rdp.server.dao.ResearcherDao;
import ubc.pavlab.rdp.server.dao.UserDao;
import ubc.pavlab.rdp.server.dao.UserGroupDao;
import ubc.pavlab.rdp.server.model.Gene;
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

    @Autowired
    GeneDao geneDao;

    @Override
    @Transactional
    public Researcher create( final Researcher researcher ) {
        return researcherDao.create( researcher );
    }

    @Override
    @Transactional
    public void update( Researcher researcher ) {
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

    /**
     * Returns true if the queryGeneId already exists in Researcher.
     * 
     * @param researcher
     * @param queryGene
     * @return
     */
    private boolean geneExists( Researcher researcher, Long queryGeneId ) {
        for ( Gene gene : researcher.getGenes() ) {
            if ( gene.getId() == queryGeneId ) return true;
        }

        return false;
    }

    @Override
    @Transactional
    public void addGenes( Researcher researcher, Collection<Gene> genes ) {

        int numGenesBefore = researcher.getGenes().size();
        boolean modified = false;

        // FIXME

        for ( Gene gene : genes ) {
            // if ( gene.getId() != null )
            // Gene persistedGene = geneDao.findOrCreate( gene );
            if ( !geneExists( researcher, gene.getId() ) ) {
                // researcher.addGene( persistedGene );
                researcher.addGene( gene );
                modified = true;
            }
        }

        if ( modified ) {
            researcherDao.update( researcher );
        }

        log.info( "Added " + ( researcher.getGenes().size() - numGenesBefore ) + " genes to Researcher " + researcher );
    }

    @Override
    @Transactional
    public void removeGenes( Researcher researcher, Collection<Gene> genes ) {

        int numGenesBefore = researcher.getGenes().size();

        for ( Gene gene : genes ) {
            if ( gene.getId() != null ) {
                researcher.removeGene( gene );
            } else {
                // FIXME search with taxon to be more precise
                Collection<Gene> matchedGenes = geneDao.findByOfficalSymbol( gene.getOfficialSymbol() );
                if ( matchedGenes.size() > 0 ) {
                    researcher.removeGene( matchedGenes.iterator().next() );
                } else {
                    log.warn( "Gene " + gene + " was not removed from Researcher " + researcher
                            + " because it was found in the database" );
                }
            }
        }

        researcherDao.update( researcher );

        log.info( "Removed " + ( numGenesBefore - researcher.getGenes().size() ) + " genes to Researcher " + researcher );
    }
}
