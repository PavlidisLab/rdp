package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;
import java.util.Optional;

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
@ToString(callSuper = true)
public class UserGene extends Gene implements PrivacySensitive {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private TierType tier;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @Transient
    private User remoteUser;

    @Column(name = "user_privacy_level", nullable = true)
    @ColumnDefault("NULL")
    @Enumerated(EnumType.ORDINAL)
    private PrivacyLevelType privacyLevel;

    public void updateGene( Gene gene ) {
        this.setGeneId( gene.getGeneId() );
        this.setSymbol( gene.getSymbol() );
        this.setTaxon( gene.getTaxon() );
        this.setName( gene.getName() );
        this.setAliases( gene.getAliases() );
        this.setModificationDate( gene.getModificationDate() );
        this.setTerms( gene.getTerms() );
    }

    public UserGene( Gene gene, User user, TierType tier, PrivacyLevelType privacyLevel ) {
        super();
        this.tier = tier;
        this.user = user;
        this.privacyLevel = privacyLevel;
        this.updateGene( gene );
    }

    public Optional<User> getOwner() {
        return Optional.of(getUser());
    }

    /**
     * Get the effective privacy level for this gene.
     *
     * This value cascades down to the user profile or the application default in case it is not set.
     */
    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return Optional.ofNullable(getPrivacyLevel()).orElse(getUser().getProfile().getPrivacyLevel());
    }
}
