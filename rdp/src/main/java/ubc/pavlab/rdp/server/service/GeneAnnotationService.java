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
import java.util.List;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAnnotation;
import ubc.pavlab.rdp.server.model.GeneOntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public interface GeneAnnotationService {

    @Secured({ "GROUP_USER" })
    public GeneAnnotation create( final GeneAnnotation geneAnnotation );

    @Secured({ "GROUP_ADMIN" })
    public void delete( GeneAnnotation geneAnnotation );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneAnnotation> findByGeneOntologyId( final String geneOntologyId );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneAnnotation> findByGeneOntologyIdAndTaxon( final String geneOntologyId, final Long taxonId );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneAnnotation> findByGene( final Gene gene );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public GeneAnnotation findByGeneAndGeneOntologyId( final Gene gene, final String geneOntologyId );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Long countGenesForGeneOntologyId( final String geneOntologyId );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Long countGenesForGeneOntologyIdAndTaxon( String geneOntologyId, Long taxonId );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneAnnotation> findByGeneLimitedByTermSize( final Gene gene, final int limit );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Map<GeneOntologyTerm, Long> findTermsAndFrequenciesByGenes( Collection<Gene> genes );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Map<GeneOntologyTerm, Long> findTermsAndFrequenciesByGenesLimitedByTermSize( Collection<Gene> genes,
            int limit );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneOntologyTerm> findTermsByGenes( Collection<Gene> genes );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Collection<GeneOntologyTerm> findTermsByGenesLimitedByTermSize( Collection<Gene> genes, int limit );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Map<GeneOntologyTerm, Long> findRelatedTerms( final Collection<Gene> genes, int minimumFrequency );

    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    public Map<GeneOntologyTerm, Long> findRelatedTermsLimitedByTermSize( final Collection<Gene> gene,
            int minimumFrequency, final int limit );

    @Secured({ "GROUP_ADMIN", "AFTER_ACL_READ" })
    public Collection<GeneAnnotation> loadAll();

    @Secured({ "GROUP_ADMIN" })
    public void updateGeneAnnotationTable( String filePath );

    @Secured({ "GROUP_ADMIN" })
    public void truncateGeneAnnotationTable();

    @Secured({ "GROUP_USER" })
    public Collection<GeneOntologyTerm> annotationToGeneOntologyId( Collection<GeneAnnotation> geneAnnotations );

    @Secured({ "GROUP_USER" })
    public Collection<Gene> annotationToGene( Collection<GeneAnnotation> geneAnnotations );

    public List<Object[]> calculateDirectSizes();

}
