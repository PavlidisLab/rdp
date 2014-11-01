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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * See /gemma-model/src/main/resources/ubic/gemma/model/common/description/BibliographicReference.hbm.xml
 * 
 * @author ptan
 * @version $Id$
 */
@Entity
@Table(name = "PUBLICATION")
public class Publication implements Serializable {

    public static Collection<Publication> emptyCollection() {
        return new ArrayList<Publication>();
    }

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private Long id;

    // meshterms
    // private Collection<String> authorList;

    @Column(name = "PubMedId")
    private int pubMedId;

    private String title;

    private Date publicationDate;

    private String fullTextUri;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    public Publication() {

    }

    public Publication( String fullTextUri, String abstractText ) {
        this.setFullTextUri( fullTextUri );
        this.abstractText = abstractText;
    }

    public Publication( int pubMedId ) {
        this.pubMedId = pubMedId;
    }

    /*
     * public Collection<String> getAuthorList() { return authorList; }
     * 
     * public void setAuthorList( Collection<String> authorList ) { this.authorList = authorList; }
     */

    public int getPubMedId() {
        return pubMedId;
    }

    public void setPubMedId( int pubMedId ) {
        this.pubMedId = pubMedId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public Date getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate( Date publicationDate ) {
        this.publicationDate = publicationDate;
    }

    public String getFullTextUri() {
        return fullTextUri;
    }

    public void setFullTextUri( String fullTextUri ) {
        this.fullTextUri = fullTextUri;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText( String abstractText ) {
        this.abstractText = abstractText;
    }
}
