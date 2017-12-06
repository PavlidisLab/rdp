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

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.json.JSONObject;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
@Entity
@Table(name = "RESEARCHER_GENE")
@AssociationOverrides({
        @AssociationOverride(name = "pk.researcher", joinColumns = @JoinColumn(name = "RESEARCHER_ID")),
        @AssociationOverride(name = "pk.gene", joinColumns = @JoinColumn(name = "GeneID")) })
public class GeneAssociation {

    @EmbeddedId
    private GeneAssociationID pk = new GeneAssociationID();

    public enum TierType {
        TIER1, TIER2, TIER3, UNKNOWN
    }

    @Enumerated(EnumType.STRING)
    @Column
    private TierType tier;

    @JsonIgnore
    public GeneAssociationID getPk() {
        return pk;
    }

    public GeneAssociation() {
    }

    public GeneAssociation( Gene gene, Researcher researcher, TierType tier ) {
        this.pk.setResearcher( researcher );
        this.pk.setGene( gene );
        this.tier = tier;
    }

    @JsonUnwrapped
    public Gene getGene() {
        return this.pk.getGene();
    }

    @JsonIgnore
    public Researcher getResearcher() {
        return this.pk.getResearcher();
    }

    public void setTier( TierType tier ) {
        this.tier = tier;
    }

    public TierType getTier() {
        return tier;
    }

    public JSONObject toJSON() {
        JSONObject jsonObj = new JSONObject();
        Gene gene = this.pk.getGene();
        jsonObj.put( "id", gene.getId() );
        jsonObj.put( "taxonId", gene.getTaxon().getId() );
        jsonObj.put( "officialSymbol", gene.getOfficialSymbol() );
        jsonObj.put( "officialName", gene.getOfficialName() );
        jsonObj.put( "aliases", gene.getAliases() );
        jsonObj.put( "tier", this.tier );
        return jsonObj;
    }

}
