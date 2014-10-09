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

package ubc.pavlab.rdp.server.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@IdClass(GeneAnnotation.class)
@Table(name = "GENE_ANNOTATION")
public class GeneAnnotation implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3154733702745000196L;

    @Column(name = "TAXON_ID")
    private Long taxonId;

    /*
     * @Id
     * 
     * @ManyToOne
     * 
     * @JoinColumn(name = "GeneID") private Gene gene;
     */

    @Id
    @Column(name = "GO_ID")
    private String geneOntologyId;

    @Column(name = "EVIDENCE", length = 64)
    private String evidence;

    @Column(name = "QUALIFIER", length = 64)
    private String qualifier;

    @Column(name = "GO_TERM", columnDefinition = "TEXT")
    private String geneOntologyTerm;

    @Column(name = "PUBMED", columnDefinition = "TEXT")
    private String pubMed;

    @Id
    @Column(name = "CATEGORY", length = 64)
    private String category;

}
