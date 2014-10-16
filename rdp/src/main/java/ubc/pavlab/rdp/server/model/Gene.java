/*
 * The aspiredb project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubc.pavlab.rdp.server.model;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * See gemma-model/src/main/java/ubic/gemma/model/genome/Gene.java
 * 
 * @author ??
 * @version $Id: Gene.java,v 1.4 2013/06/11 22:30:36 anton Exp $
 */
@Entity
@Table(name = "GENE")
public class Gene implements Comparable<Gene> {

    @Id
    @Column(name = "GeneID")
    private Long id;

    @Column(name = "tax_id")
    private Long taxonId;

    @Column(name = "Symbol")
    private String officialSymbol;

    @Column(name = "description", columnDefinition = "TEXT")
    private String officialName;

    @Column(name = "Synonyms", columnDefinition = "TEXT")
    private String aliases;

    @Column(name = "Modification_date")
    private int modificationDate;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "pk.gene")
    private Set<GeneAssociation> geneAssociations = new HashSet<GeneAssociation>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "pk.gene")
    private Set<GeneAnnotation> geneAnnotations = new HashSet<GeneAnnotation>();

    // Phenotype ????

    @Override
    public String toString() {
        return "id=" + id + " symbol=" + officialSymbol + " taxon=" + taxonId + " hashCode=" + hashCode();
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
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
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
        if ( !( obj instanceof Gene ) ) return false;
        Gene other = ( Gene ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public Gene() {
    }

    public Gene( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public Gene( Long ncbiGeneId, Long taxonId, String officialSymbol, String officialName, String aliases ) {
        this.id = ncbiGeneId;
        this.taxonId = taxonId;
        this.officialSymbol = officialSymbol;
        this.officialName = officialName;
        this.aliases = aliases;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getOfficialName() {
        return officialName;
    }

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases( String aliases ) {
        this.aliases = aliases;
    }

    public String getOfficialSymbol() {
        return officialSymbol;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    @Override
    public int compareTo( Gene otherGene ) {
        return this.getId().compareTo( otherGene.getId() );
    }
}
