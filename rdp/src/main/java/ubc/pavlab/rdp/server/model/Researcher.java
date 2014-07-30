/*
 * The rdp project
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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;

@Entity
@Table(name = "RESEARCHER")
public class Researcher implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 778565921919207933L;

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @OneToOne(cascade = { CascadeType.ALL })
    @JoinColumn(name = "CONTACT_FK")
    private User contact;

    @Column(name = "ORGANIZATION")
    private String organization;

    @Column(name = "DEPARTMENT")
    private String department;

    @Column(name = "PHONE")
    private String phone;

    @Column(name = "WEBSITE")
    private String website;

    public void setContact( User contact ) {
        this.contact = contact;
    }

    public User getContact() {
        return this.contact;
    }

    public void setDepartment( String department ) {
        this.department = department;
    }

    public void setOrganization( String organization ) {
        this.organization = organization;
    }

    public String getDepartment() {
        return department;
    }

    public String getOrganization() {
        return organization;
    }

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

    public Object getId() {
        return this.id;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone( String phone ) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite( String website ) {
        this.website = website;
    }

}
