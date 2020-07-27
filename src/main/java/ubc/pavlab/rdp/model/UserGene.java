package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.security.PrivacySensitive;

import javax.persistence.*;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.Set;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "gene_id" }) },
        indexes = {
                @Index(columnList = "gene_id"),
                @Index(columnList = "gene_id, tier", name = "gene_id_tier_hidx"),
                @Index(columnList = "symbol, taxon_id, tier", name = "symbol_taxon_id_tier_hidx") })
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(of = { "user", "geneId" }, callSuper = false)
@CommonsLog
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
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Transient
    private User remoteUser;

    @Column(name = "user_privacy_level")
    @ColumnDefault("NULL")
    @Enumerated(EnumType.ORDINAL)
    private PrivacyLevelType privacyLevel;

    @ManyToOne
    @JoinColumn(name = "gene_id", referencedColumnName = "gene_id", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    @JsonIgnore
    private GeneInfo geneInfo;

    public static UserGene createUserGeneFromGene( Gene gene, User user, TierType tier, PrivacyLevelType privacyLevel ) {
        UserGene userGene = new UserGene();
        userGene.updateGene(gene);
        userGene.setUser(user);
        userGene.setTier(tier);
        userGene.setPrivacyLevel( privacyLevel );
        return userGene;
    }

    public void updateGene( Gene gene ) {
        this.setGeneId( gene.getGeneId() );
        this.setSymbol( gene.getSymbol() );
        this.setTaxon( gene.getTaxon() );
        this.setName( gene.getName() );
        this.setAliases( gene.getAliases() );
        this.setModificationDate( gene.getModificationDate() );
        this.setTerms( gene.getTerms() );
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
        PrivacyLevelType privacyLevel = Optional.ofNullable(getPrivacyLevel()).orElse(getUser().getProfile().getPrivacyLevel());
        if ( privacyLevel.ordinal() > getUser().getEffectivePrivacyLevel().ordinal() ) {
            log.warn( MessageFormat.format( "Effective gene privacy level {0} of {1} is higher than that of the user {2}, and will be capped to {3}.",
                    privacyLevel, this, getUser(), getUser().getEffectivePrivacyLevel() ) );
            return getUser().getEffectivePrivacyLevel();
        }
        return privacyLevel;
    }
}
