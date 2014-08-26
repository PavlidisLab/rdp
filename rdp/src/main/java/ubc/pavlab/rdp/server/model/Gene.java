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

import org.json.JSONObject;

/**
 * See gemma-model/src/main/java/ubic/gemma/model/genome/Gene.java
 * 
 * @author ??
 * @version $Id: Gene.java,v 1.4 2013/06/11 22:30:36 anton Exp $
 */
@Entity
@Table(name = "GENE")
public class Gene {

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

    private Taxon taxon;

    // Phenotype ????

    @Override
    public String toString() {
        return "id=" + id + " symbol=" + officialSymbol + " taxon=" + taxon + " hashCode=" + hashCode();
    }

    public Gene() {
    }

    public Gene( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
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

    public Taxon getTaxon() {
        return taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public Set<GeneAlias> getAliases() {
        return aliases;
    }

    public void setAliases( Set<GeneAlias> aliases ) {
        this.aliases = aliases;
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
        setEnsemblId( jsonObj.get( "ensemblId" ).toString() );
        setNcbiGeneId( jsonObj.get( "ncbiGeneId" ).toString() );

        // TODO Set Aliases, Taxon
        // setTaxon( jsonObj.get( "taxon" ).toString() );
    }
}
