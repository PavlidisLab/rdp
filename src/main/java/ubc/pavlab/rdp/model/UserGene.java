package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "gene_id"})},
        indexes = {@Index(columnList = "gene_id, tier", name = "gene_id_tier_hidx"),
                @Index(columnList = "symbol, taxon_id, tier", name = "symbol_taxon_id_tier_hidx")}
)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserGene extends Gene {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private TierType tier;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    public void updateGene( Gene gene ) {
        this.setGeneId( gene.getGeneId() );
        this.setSymbol( gene.getSymbol() );
        this.setTaxon( gene.getTaxon() );
        this.setName( gene.getName() );
        this.setAliases( gene.getAliases() );
        this.setModificationDate( gene.getModificationDate() );
        this.setTerms( gene.getTerms() );
    }

    public UserGene( Gene gene, User user, TierType tier ) {
        super();
        this.tier = tier;
        this.user = user;
        this.updateGene( gene );
    }

}
