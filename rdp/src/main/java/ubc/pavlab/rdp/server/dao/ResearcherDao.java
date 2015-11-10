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

package ubc.pavlab.rdp.server.dao;

import java.util.Collection;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAssociation.TierType;
import ubc.pavlab.rdp.server.model.Researcher;

/**
 * TODO Document Me
 * 
 * @author ptan
 * @version $Id$
 */
public interface ResearcherDao extends DaoBase<Researcher> {

    public Researcher findByEmail( final String email );

    public Researcher findByUsername( final String username );

    public Collection<Researcher> findByGene( final Gene gene );

    public Collection<Researcher> findByGene( final Gene gene, final TierType tier );

    public Researcher thaw( Researcher researcher );

    public Long countResearchersWithGenes();

    public Long countResearchers();

}
