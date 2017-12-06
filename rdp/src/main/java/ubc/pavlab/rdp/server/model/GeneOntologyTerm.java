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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ubc.pavlab.rdp.server.service.GOServiceImpl.GOAspect;
import ubic.basecode.ontology.model.OntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@Table(name = "GOTERMS")
public class GeneOntologyTerm {

    @JsonIgnore
    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(name = "GO_ID")
    private String geneOntologyId;

    @Column(name = "TAXON_ID")
    private Long taxonId;

    @Column(name = "GO_TERM", columnDefinition = "TEXT")
    private String geneOntologyTerm;

    @Column(name = "frequency")
    private Long frequency;

    @Column(name = "size")
    private Long size;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "aspect")
    private GOAspect aspect;

    public Long getId() {
        return id;
    }

    public GeneOntologyTerm() {

    }

    public GeneOntologyTerm( String geneOntologyId, String geneOntologyTerm ) {
        this.geneOntologyId = geneOntologyId;
        this.geneOntologyTerm = geneOntologyTerm;
    }

    public GeneOntologyTerm( OntologyTerm term ) {
        this.geneOntologyId = term.getUri().replaceAll( ".*?/", "" ).replace( "_", ":" );
        this.geneOntologyTerm = term.getTerm();
    }

    public GeneOntologyTerm( GOTerm term ) {
        this.geneOntologyId = term.getId();
        this.geneOntologyTerm = term.getTerm();
        this.definition = term.getDefinition();
        this.aspect = GOAspect.valueOf( term.getAspect().toUpperCase() );
    }

    public GeneOntologyTerm( GeneOntologyTerm term ) {
        this.geneOntologyId = term.geneOntologyId;
        this.geneOntologyTerm = term.geneOntologyTerm;
        this.taxonId = term.taxonId;
        this.frequency = term.frequency;
        this.size = term.size;
        this.definition = term.definition;
        this.aspect = term.aspect;
    }

    /**
     * @return the geneOntologyId
     */
    public String getGeneOntologyId() {
        return geneOntologyId;
    }

    /**
     * @param geneOntologyId the geneOntologyId to set
     */
    public void setGeneOntologyId( String geneOntologyId ) {
        this.geneOntologyId = geneOntologyId;
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

    public String getDefinition() {
        return definition;
    }

    public void setDefinition( String definition ) {
        this.definition = definition;
    }

    public GOAspect getAspect() {
        return aspect;
    }

    public void setAspect( GOAspect aspect ) {
        this.aspect = aspect;
    }

    /**
     * @return the frequency
     */
    public Long getFrequency() {
        return frequency;
    }

    /**
     * @param frequency the frequency to set
     */
    public void setFrequency( Long frequency ) {
        this.frequency = frequency;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize( Long size ) {
        this.size = size;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
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
        result = prime * result + ( ( geneOntologyId == null ) ? 0 : geneOntologyId.hashCode() );
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
        if ( !( obj instanceof GeneOntologyTerm ) ) return false;
        GeneOntologyTerm other = ( GeneOntologyTerm ) obj;
        if ( geneOntologyId == null ) {
            if ( other.geneOntologyId != null ) return false;
        } else if ( !geneOntologyId.equals( other.geneOntologyId ) ) return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "GeneOntologyTerm [geneOntologyId=" + geneOntologyId + "]";
    }

}
