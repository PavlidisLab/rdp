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

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Embeddable
public class GeneAnnotationID implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5458625738112267867L;

    private String geneOntologyId;

    @ManyToOne
    private Gene gene;

    /**
     * @return the geneOntologyId
     */
    public String getGeneOntologyId() {
        return geneOntologyId;
    }

    /**
     * @param geneOntologyId the geneOntologyId to set
     */
    public void setGeneOntologyId( String geneOntologyId ) {
        this.geneOntologyId = geneOntologyId;
    }

    /**
     * @return the gene
     */
    public Gene getGene() {
        return gene;
    }

    /**
     * @param gene the gene to set
     */
    public void setGene( Gene gene ) {
        this.gene = gene;
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
        result = prime * result + ( ( gene == null ) ? 0 : gene.hashCode() );
        result = prime * result + ( ( geneOntologyId == null ) ? 0 : geneOntologyId.hashCode() );
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
        if ( !( obj instanceof GeneAnnotationID ) ) return false;
        GeneAnnotationID other = ( GeneAnnotationID ) obj;
        if ( gene == null ) {
            if ( other.gene != null ) return false;
        } else if ( !gene.equals( other.gene ) ) return false;
        if ( geneOntologyId == null ) {
            if ( other.geneOntologyId != null ) return false;
        } else if ( !geneOntologyId.equals( other.geneOntologyId ) ) return false;
        return true;
    }

}
