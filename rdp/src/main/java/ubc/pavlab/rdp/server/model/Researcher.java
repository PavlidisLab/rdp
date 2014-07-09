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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;

@Entity
@Table(name = "RESEARCHER")
@PrimaryKeyJoinColumn(name="ID")
public class Researcher extends User {

    public static Collection<Researcher> emptyCollection() {
        return new ArrayList<Researcher>();
    }

    @ManyToMany
    @JoinTable(name = "RESEARCHER_TAXONS", joinColumns = { @JoinColumn(name = "RESEARCHER_ID", referencedColumnName = "ID") }, inverseJoinColumns = { @JoinColumn(name = "TAXON_ID", referencedColumnName = "ID") })
    private List<Taxon> taxons = new ArrayList<Taxon>();

    @ManyToMany
    @JoinTable(name = "RESEARCHER_GENES", joinColumns = { @JoinColumn(name = "RESEARCHER_ID", referencedColumnName = "ID") }, inverseJoinColumns = { @JoinColumn(name = "GENE_ID", referencedColumnName = "ID") })
    private List<Gene> genes = new ArrayList<Gene>();

    @ManyToMany
    @JoinTable(name = "RESEARCHER_PUBLICATIONS", joinColumns = { @JoinColumn(name = "RESEARCHER_ID", referencedColumnName = "ID") }, inverseJoinColumns = { @JoinColumn(name = "PUBLICATION_ID", referencedColumnName = "ID") })
    private List<Publication> publications = new ArrayList<Publication>();


}
