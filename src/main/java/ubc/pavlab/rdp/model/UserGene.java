package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import org.json.JSONObject;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "user_gene")
@AssociationOverrides({
        @AssociationOverride(name = "pk.user", joinColumns = @JoinColumn(name = "user_id")),
        @AssociationOverride(name = "pk.gene", joinColumns = @JoinColumn(name = "gene_id")) })
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"pk"})
@ToString
public class UserGene {

    public enum TierType {
        TIER1, TIER2, TIER3, UNKNOWN
    }

    @JsonIgnore
    @EmbeddedId
    private UserGeneId pk = new UserGeneId();

    @Enumerated(EnumType.STRING)
    @Column
    private TierType tier;

    @JsonIgnore
    @Transient
    public User getUser() {
        return getPk().getUser();
    }

    public void setUser(User user) {
        getPk().setUser( user );
    }

    @JsonUnwrapped
    @Transient
    public Gene getGene() {
        return getPk().getGene();
    }

    public void setGene(Gene gene) {
        getPk().setGene( gene );
    }

    public JSONObject toJSON() {
        JSONObject jsonObj = new JSONObject();
        Gene gene = this.pk.getGene();
        jsonObj.put( "id", gene.getId() );
        jsonObj.put( "taxonId", gene.getTaxon().getId() );
        jsonObj.put( "symbol", gene.getSymbol() );
        jsonObj.put( "name", gene.getName() );
        jsonObj.put( "aliases", gene.getAliases() );
        jsonObj.put( "tier", this.tier );
        return jsonObj;
    }
}
