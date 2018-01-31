package ubc.pavlab.rdp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserGene extends Gene {

    @Enumerated(EnumType.STRING)
    @Column
    private TierType tier;

    public void updateGene(Gene gene) {
        this.setId( gene.getId() );
        this.setSymbol( gene.getSymbol() );
        this.setTaxon( gene.getTaxon() );
        this.setName( gene.getName() );
        this.setAliases( gene.getAliases() );
        this.setModificationDate( gene.getModificationDate() );
        this.setTerms( gene.getTerms() );
    }

    public UserGene(Gene gene, TierType tier) {
        super();
        this.tier = tier;
        this.updateGene( gene );
    }

}
