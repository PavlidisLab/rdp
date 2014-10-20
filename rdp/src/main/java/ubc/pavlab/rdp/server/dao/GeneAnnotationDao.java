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
import ubc.pavlab.rdp.server.model.GeneAnnotation;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface GeneAnnotationDao extends DaoBase<GeneAnnotation> {

    public Collection<GeneAnnotation> findByGeneOntologyId( final String geneOntologyId );

    public Collection<GeneAnnotation> findByGene( final Gene gene );

    public GeneAnnotation findByGeneAndGeneOntologyId( final Gene gene, final String geneOntologyId );

    public Long countGenesForGeneOntologyId( final String geneOntologyId );

    public Long countGenesForGeneOntologyIdAndTaxon( String geneOntologyId, Long taxonId );

    public Collection<GeneAnnotation> loadAll();

    public void updateGeneAnnotationTable( String filePath );

    public void truncateGeneAnnotationTable();

    public Collection<GeneAnnotation> findByGeneOntologyIdAndTaxon( String geneOntologyId, Long taxonId );

}
