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

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "TAXON")
public class Taxon implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -6156768802997174217L;

    @Id
    @Column(name = "tax_id")
    private Long id;

    private String scientificName;

    private String commonName;

    @JsonIgnore
    private boolean isActivated;

    public Taxon() {
    }

    public Taxon( Long ncbiId ) {
        this.id = ncbiId;
    }

    public Taxon( Long ncbiId, String scientificName, String commonName ) {
        this.scientificName = scientificName;
        this.commonName = commonName;
        this.id = ncbiId;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
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

    public boolean getIsActivated() {
        return isActivated;
    }

    public void setIsActivated( boolean isActivated ) {
        this.isActivated = isActivated;
    }

}
