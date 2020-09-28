package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Transient;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;
import javax.validation.Valid;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "user")
@Cacheable
// @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "id" })
@ToString(of = { "id", "email", "enabled" })
@CommonsLog
public class User implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "email")
    @Email(message = "Your email address is not valid.")
    @NotEmpty(message = "Please provide an email address.")
    private String email;

    @Column(name = "password")
    @Length(min = 6, message = "Your password must have at least 6 characters.")
    @NotEmpty(message = "Please provide your password.")
    @JsonIgnore
    @Transient
    private String password;

    @Column(name = "enabled")
    @JsonIgnore
    private boolean enabled;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();

    @Valid
    @Embedded
    @JsonUnwrapped
    private Profile profile;

    @Transient
    private String origin;

    @Transient
    private String originUrl;

    /* Research related information */

    // @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @ElementCollection()
    @CollectionTable(name = "descriptions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyJoinColumn(name = "taxon_id")
    @Column(name = "description", columnDefinition = "TEXT")
    @JsonIgnore
    private Map<Taxon, String> taxonDescriptions = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Set<UserTerm> userTerms = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @MapKey(name = "geneId")
    private Map<Integer, UserGene> userGenes = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @MapKey(name = "uberonId")
    private Map<String, UserOrgan> userOrgans = new HashMap<>();

    @JsonIgnore
    @Transient
    public Set<UserGene> getGenesByTaxon( Taxon taxon ) {
        return this.getUserGenes().values().stream().filter( gene -> gene.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<UserGene> getGenesByTaxonAndTier( Taxon taxon, Set<TierType> tiers ) {
        return this.getUserGenes().values().stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) && tiers.contains( gene.getTier() ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<UserTerm> getTermsByTaxon( Taxon taxon ) {
        return this.getUserTerms().stream().filter( term -> term.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public boolean hasTaxon( @NonNull Taxon taxon ) {
        return this.getUserGenes().values().stream().anyMatch( g -> g.getTaxon().equals( taxon ) );
    }

    @JsonIgnore
    @Transient
    public Set<Taxon> getTaxons() {
        return this.getUserGenes().values().stream().map( Gene::getTaxon ).collect( Collectors.toSet() );
    }

    @Override
    @JsonIgnore
    public Optional<User> getOwner() {
        return Optional.of( this );
    }

    @Override
    @JsonIgnore
    public PrivacyLevelType getEffectivePrivacyLevel() {
        // this is a fallback
        if ( getProfile() == null || getProfile().getPrivacyLevel() == null ) {
            log.warn( MessageFormat.format( "User {0} has no profile or a null privacy level defined in its profile.", this ) );
            return PrivacyLevelType.PRIVATE;
        }
        return getProfile().getPrivacyLevel();
    }
}
