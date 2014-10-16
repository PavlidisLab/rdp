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

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@Table(name = "GENE_ANNOTATION")
@AssociationOverrides({ @AssociationOverride(name = "pk.geneOntologyId", joinColumns = @JoinColumn(name = "GO_ID")),
        @AssociationOverride(name = "pk.gene", joinColumns = @JoinColumn(name = "GeneID")) })
public class GeneAnnotation {

    @EmbeddedId
    private GeneAnnotationID pk = new GeneAnnotationID();

    @Column(name = "TAXON_ID")
    private Long taxonId;

    @Column(name = "EVIDENCE", length = 64)
    private String evidence;

    @Column(name = "QUALIFIER", length = 64)
    private String qualifier;

    @Column(name = "GO_TERM", columnDefinition = "TEXT")
    private String geneOntologyTerm;

    @Column(name = "PUBMED", columnDefinition = "TEXT")
    private String pubMed;

    @Column(name = "CATEGORY", length = 64)
    private String category;

    public GeneAnnotationID getPk() {
        return pk;
    }

    public GeneAnnotation() {
    }

    public GeneAnnotation( Gene gene, String geneOntologyId, Long taxonId, String geneOntologyTerm, String category ) {
        this.pk.setGene( gene );
        this.pk.setGeneOntologyId( geneOntologyId );
        this.taxonId = taxonId;
        this.geneOntologyTerm = geneOntologyTerm;
        this.category = category;
    }

    /**
     * @return the taxonId
     */
    public Long getTaxonId() {
        return taxonId;
    }

    /**
     * @param taxonId the taxonId to set
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    /**
     * @return the gene
     */
    public Gene getGene() {
        return this.pk.getGene();
    }

    /**
     * @return the geneOntologyId
     */
    public String getGeneOntologyId() {
        return this.pk.getGeneOntologyId();
    }

    /**
     * @return the evidence
     */
    public String getEvidence() {
        return evidence;
    }

    /**
     * @param evidence the evidence to set
     */
    public void setEvidence( String evidence ) {
        this.evidence = evidence;
    }

    /**
     * @return the qualifier
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @param qualifier the qualifier to set
     */
    public void setQualifier( String qualifier ) {
        this.qualifier = qualifier;
    }

    /**
     * @return the geneOntologyTerm
     */
    public String getGeneOntologyTerm() {
        return geneOntologyTerm;
    }

    /**
     * @param geneOntologyTerm the geneOntologyTerm to set
     */
    public void setGeneOntologyTerm( String geneOntologyTerm ) {
        this.geneOntologyTerm = geneOntologyTerm;
    }

    /**
     * @return the pubMed
     */
    public String getPubMed() {
        return pubMed;
    }

    /**
     * @param pubMed the pubMed to set
     */
    public void setPubMed( String pubMed ) {
        this.pubMed = pubMed;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory( String category ) {
        this.category = category;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( pk == null ) ? 0 : pk.hashCode() );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !( obj instanceof GeneAnnotation ) ) return false;
        GeneAnnotation other = ( GeneAnnotation ) obj;
        if ( pk == null ) {
            if ( other.pk != null ) return false;
        } else if ( !pk.equals( other.pk ) ) return false;
        return true;
    }

}
