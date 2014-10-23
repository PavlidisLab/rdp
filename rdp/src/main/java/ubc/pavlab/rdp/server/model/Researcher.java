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
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.common.auditAndSecurity.User;
import ubc.pavlab.rdp.server.service.TaxonService;

@Entity
@Table(name = "RESEARCHER")
public class Researcher implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 778565921919207933L;

    private static Log log = LogFactory.getLog( Researcher.class );

    @Autowired
    @Transient
    TaxonService taxonService;

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    @OneToOne(cascade = { CascadeType.ALL })
    @JoinColumn(name = "CONTACT_FK")
    private User contact;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

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

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getDescription() {
        return description;
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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "pk.researcher", orphanRemoval = true)
    private Set<GeneAssociation> geneAssociations = new HashSet<GeneAssociation>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "RESEARCHER_ID")
    private Set<TaxonDescription> taxonDescriptions = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "RESEARCHER_ID")
    private Set<GeneOntologyTerm> goTerms = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "RESEARCHER_TAXONS", joinColumns = { @JoinColumn(name = "RESEARCHER_ID") }, inverseJoinColumns = { @JoinColumn(name = "TAXON_ID") })
    private Set<Taxon> taxons = new HashSet<>();

    /*
     * @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
     * 
     * @JoinTable(name = "RESEARCHER_GENES", joinColumns = { @JoinColumn(name = "RESEARCHER_ID") }, inverseJoinColumns =
     * { @JoinColumn(name = "GENE_ID") }) private Set<Gene> genes = new HashSet<>();
     */

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "RESEARCHER_PUBLICATIONS", joinColumns = { @JoinColumn(name = "RESEARCHER_ID") }, inverseJoinColumns = { @JoinColumn(name = "PUBLICATION_ID") })
    private Set<Publication> publications = new HashSet<>();

    public Object getId() {
        return this.id;
    }

    @Override
    public boolean equals( Object object ) {

        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Researcher ) ) {
            return false;
        }
        final Researcher that = ( Researcher ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    @Override
    public String toString() {
        return "id=" + id + " username=" + contact.getUserName();
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

    public Set<GeneOntologyTerm> getGoTerms() {
        return goTerms;
    }

    public void setGoTerms( Set<GeneOntologyTerm> goTerms ) {
        this.goTerms = goTerms;
    }

    public Collection<GeneAssociation> getGeneAssociations() {
        return geneAssociations;
    }

    public Collection<TaxonDescription> getTaxonDescriptions() {
        return taxonDescriptions;
    }

    public Collection<Taxon> getTaxons() {
        return taxons;
    }

    public Collection<Publication> getPublications() {
        return publications;
    }

    public void setTaxon( Set<Taxon> taxons ) {
        this.taxons = taxons;
    }

    public void setGeneAssociations( Set<GeneAssociation> genes ) {
        this.geneAssociations = genes;
    }

    public void updateTaxonDescription( Long taxonId, String description ) {
        for ( TaxonDescription td : this.taxonDescriptions ) {
            if ( td.getTaxonId().equals( taxonId ) ) {
                td.setDescription( description );
                return;
            }
        }
        this.taxonDescriptions.add( new TaxonDescription( taxonId, description ) );
    }

    public void setPublications( Set<Publication> publications ) {
        this.publications = publications;
    }

    public boolean addGeneAssociation( final GeneAssociation gene ) {
        return this.geneAssociations.add( gene );
    }

    public boolean removeGeneAssociation( final GeneAssociation gene ) {
        return this.geneAssociations.remove( gene );
    }

    public boolean addGOTerm( final GeneOntologyTerm term ) {
        return this.goTerms.add( term );
    }

    public boolean removeGOTerm( final GeneOntologyTerm term ) {
        return this.goTerms.remove( term );
    }

    public boolean addPublication( final Publication publication ) {
        return this.publications.add( publication );
    }

    public void removePublication( final Publication publication ) {
        this.publications.remove( publication );
    }

    public Collection<Gene> getGenes() {
        Collection<Gene> genes = new HashSet<Gene>();
        for ( GeneAssociation g : geneAssociations ) {
            genes.add( g.getGene() );
        }

        return genes;
    }

    public Collection<Gene> getGenesByTaxonId( Long taxonId ) {
        Collection<Gene> genes = new HashSet<Gene>();
        for ( GeneAssociation ga : geneAssociations ) {
            Gene g = ga.getGene();
            if ( g.getTaxonId().equals( taxonId ) ) {
                genes.add( g );
            }
        }

        return genes;
    }

    public Collection<GeneAssociation> getGeneAssociatonsFromGenes( Collection<Gene> genes ) {
        Collection<GeneAssociation> geneAssociations = new HashSet<GeneAssociation>();
        for ( Gene g : genes ) {
            for ( GeneAssociation gA : this.geneAssociations ) {
                if ( gA.getGene().getId() == g.getId() ) {
                    geneAssociations.add( gA );
                }
            }
        }

        return geneAssociations;
    }

    public GeneAssociation getGeneAssociatonFromGene( Gene gene ) {
        for ( GeneAssociation gA : this.geneAssociations ) {
            if ( gA.getGene().getId().equals( gene.getId() ) ) {
                return gA;
            }
        }

        return null;
    }

}
