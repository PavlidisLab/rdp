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
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@Table(name = "RESEARCHER_DESCRIPTIONS")
public class TaxonDescription {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column()
    private String taxon;

    public TaxonDescription() {
    }

    public TaxonDescription( String taxon, String description ) {
        this.description = description;
        this.taxon = taxon;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getTaxon() {
        return taxon;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public long getId() {
        return id;
    }

}