package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.NaturalId;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "gene_id" }) },
        indexes = {
                @Index(columnList = "gene_id"),
                @Index(columnList = "gene_id, tier"),
                @Index(columnList = "symbol, taxon_id, tier") })
@Cacheable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "user" }, callSuper = true)
@CommonsLog
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@ToString(of = { "user", "tier", "privacyLevel" }, callSuper = true)
public class UserGene extends Gene implements UserContent {

    /**
     * Obtain a comparator for comparing {@link UserGene}.
     *
     * <ul>
     *     <li>by anonymity, anonymous users are displayed after</li>
     *     <li>by taxon, see {@link Taxon#getComparator()}</li>
     *     <li>by tier</li>
     *     <li>by user, see {@link User#getComparator()}</li>
     * </ul>
     */
    public static Comparator<UserGene> getComparator() {
        return Comparator.comparing( UserGene::getTaxon, Taxon.getComparator() )
                .thenComparing( UserGene::getTier )
                .thenComparing( UserGene::getUser, User.getComparator() );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @Transient
    private UUID anonymousId;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private TierType tier;

    @NaturalId // alongside geneId defined in the parent class
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Transient
    private User remoteUser;

    @Column(name = "user_privacy_level")
    @ColumnDefault("NULL")
    @Enumerated(EnumType.ORDINAL)
    @JsonIgnore
    private PrivacyLevelType privacyLevel;

    @ManyToOne
    @JoinColumn(name = "gene_id", referencedColumnName = "gene_id", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
    @JsonIgnore
    private GeneInfo geneInfo;

    public static UserGene createUserGeneFromGene( Gene gene, User user, TierType tier, PrivacyLevelType privacyLevel ) {
        UserGene userGene = new UserGene();
        userGene.updateGene( gene );
        userGene.setUser( user );
        userGene.setTier( tier );
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
    }

    @Override
    @JsonIgnore
    public Optional<User> getOwner() {
        return Optional.of( getUser() );
    }

    /**
     * Get the effective privacy level for this gene.
     * <p>
     * This value cascades down to the user profile or the application default in case it is not set.
     */
    @Override
    @JsonIgnore
    public PrivacyLevelType getEffectivePrivacyLevel() {
        PrivacyLevelType privacyLevel = getPrivacyLevel() != null ? getPrivacyLevel() : getUser().getProfile().getPrivacyLevel();
        if ( privacyLevel.ordinal() > getUser().getEffectivePrivacyLevel().ordinal() ) {
            log.warn( MessageFormat.format( "Gene privacy level {0} of {1} is looser than that of the user profile {2}, and will be capped to {3}.",
                    privacyLevel, this, getUser(), getUser().getEffectivePrivacyLevel() ) );
            return getUser().getEffectivePrivacyLevel();
        }
        return privacyLevel;
    }
}
