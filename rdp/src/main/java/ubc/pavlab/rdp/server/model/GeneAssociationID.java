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

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Embeddable
public class GeneAssociationID implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 7241936203064995188L;

    @ManyToOne
    private Researcher researcher;

    @ManyToOne
    private Gene gene;

    public Researcher getResearcher() {
        return researcher;
    }

    public void setResearcher( Researcher researcher ) {
        this.researcher = researcher;
    }

    public Gene getGene() {
        return gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

}