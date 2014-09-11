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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.json.JSONArray;
import org.json.JSONObject;

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
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    private String officialName;

    private String officialSymbol;

    private String ncbiGeneId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "GENE_FK")
    private Set<GeneAlias> aliases = new HashSet<>();

    private String ensemblId;

    private String taxon;

    // Phenotype ????

    @Override
    public String toString() {
        return "id=" + id + " symbol=" + officialSymbol + " taxon=" + taxon + " ncbi=" + ncbiGeneId + " hashCode="
                + hashCode();
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
        result = prime * result + ( ( officialSymbol == null ) ? 0 : officialSymbol.hashCode() );
        result = prime * result + ( ( taxon == null ) ? 0 : taxon.hashCode() );
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
        if ( officialSymbol == null ) {
            if ( other.officialSymbol != null ) return false;
        } else if ( !officialSymbol.equals( other.officialSymbol ) ) return false;
        if ( taxon == null ) {
            if ( other.taxon != null ) return false;
        } else if ( !taxon.equals( other.taxon ) ) return false;
        return true;
    }

    public Gene() {
    }

    public Gene( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public Gene( String ensemblId, String taxon, String officialSymbol, String officialName ) {
        this.ensemblId = ensemblId;
        this.taxon = taxon;
        this.officialSymbol = officialSymbol;
        this.officialName = officialName;
    }

    public Long getId() {
        return id;
    }

    public String getOfficialName() {
        return officialName;
    }

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    public String getNcbiGeneId() {
        return ncbiGeneId;
    }

    public void setNcbiGeneId( String ncbiGeneId ) {
        this.ncbiGeneId = ncbiGeneId;
    }

    public String getEnsemblId() {
        return ensemblId;
    }

    public void setEnsemblId( String ensemblId ) {
        this.ensemblId = ensemblId;
    }

    public String getTaxon() {
        return taxon;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public Set<GeneAlias> getAliases() {
        return aliases;
    }

    public void setAliases( Set<GeneAlias> aliases ) {
        this.aliases = aliases;
    }

    public void parseAliases( String aliases ) {
        for ( String alias : aliases.split( "," ) ) {
            this.getAliases().add( new GeneAlias( alias.trim() ) );
        }
    }

    public String getOfficialSymbol() {
        return officialSymbol;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    /**
     * Initialize fields from JSON
     * 
     * @param json
     */
    public void parseJSON( String json ) {

        JSONObject jsonObj = new JSONObject( json );

        setOfficialName( jsonObj.get( "officialName" ).toString() );
        setOfficialSymbol( jsonObj.get( "officialSymbol" ).toString() );
        // setEnsemblId( jsonObj.get( "ensemblId" ).toString() );
        setTaxon( jsonObj.get( "taxon" ).toString() );
        setNcbiGeneId( jsonObj.get( "ncbiGeneId" ).toString() );

        @SuppressWarnings("unchecked")
        JSONArray aliases = ( JSONArray ) jsonObj.get( "aliases" );
        for ( int i = 0; i < aliases.length(); i++ ) {
            JSONObject alias = ( JSONObject ) aliases.get( i );
            getAliases().add( new GeneAlias( alias.get( "alias" ).toString() ) );
        }
    }

    @Override
    public int compareTo( Gene otherGene ) {
        return this.ncbiGeneId.compareTo( otherGene.getNcbiGeneId() );
    }
}
