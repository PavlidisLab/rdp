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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "TAXON")
public class Taxon implements Serializable {

    public static Collection<Taxon> emptyCollection() {
        return new ArrayList<Taxon>();
    }

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    private String scientificName;

    private String commonName;

    private String abbreviation;

    private String unigenePrefix;

    private String swissProtSuffix;

    private int ncbiId;

    private boolean isSpecies;

    private boolean isGenesUsable;

    private int secondaryNcbiId;

    // external database?

    @OneToOne(cascade = { CascadeType.ALL })
    @JoinColumn(name = "taxon_id")
    private Taxon parentTaxon;

    public Taxon() {
    }

    public Taxon( String scientificName, String commonName, int ncbiId ) {
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.ncbiId = ncbiId;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName( String scientificName ) {
        this.scientificName = scientificName;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName( String commonName ) {
        this.commonName = commonName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation( String abbreviation ) {
        this.abbreviation = abbreviation;
    }

    public String getUnigenePrefix() {
        return unigenePrefix;
    }

    public void setUnigenePrefix( String unigenePrefix ) {
        this.unigenePrefix = unigenePrefix;
    }

    public String getSwissProtSuffix() {
        return swissProtSuffix;
    }

    public void setSwissProtSuffix( String swissProtSuffix ) {
        this.swissProtSuffix = swissProtSuffix;
    }

    public int getNcbiId() {
        return ncbiId;
    }

    public void setNcbiId( int ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public boolean isSpecies() {
        return isSpecies;
    }

    public void setSpecies( boolean isSpecies ) {
        this.isSpecies = isSpecies;
    }

    public boolean isGenesUsable() {
        return isGenesUsable;
    }

    public void setGenesUsable( boolean isGenesUsable ) {
        this.isGenesUsable = isGenesUsable;
    }

    public int getSecondaryNcbiId() {
        return secondaryNcbiId;
    }

    public void setSecondaryNcbiId( int secondaryNcbiId ) {
        this.secondaryNcbiId = secondaryNcbiId;
    }

    public Taxon getParentTaxon() {
        return parentTaxon;
    }

    public void setParentTaxon( Taxon parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }
}
