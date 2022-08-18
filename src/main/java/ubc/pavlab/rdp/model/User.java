package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.validator.constraints.Email;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.OntologyTerm;
import ubc.pavlab.rdp.model.ontology.UserOntologyTerm;

import javax.mail.internet.InternetAddress;
import javax.persistence.*;
import javax.validation.Valid;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user")
@Cacheable
@Getter
@Setter
@Builder
@EqualsAndHashCode(of = { "email", "originUrl" })
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = { "id", "anonymousId", "email", "enabled" })
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@CommonsLog
public class User implements UserContent, Serializable {

    /**
     * Constraints for regular user accounts.
     */
    public interface ValidationUserAccount {
    }

    /**
     * Constraints for service accounts.
     */
    public interface ValidationServiceAccount {
    }

    public static Comparator<User> getComparator() {
        return Comparator.comparing( u -> u.getProfile().getFullName() );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    private Integer id;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID anonymousId;

    @NaturalId
    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "Your email address is not valid.", groups = { ValidationUserAccount.class })
    @NotNull(message = "Please provide an email address.", groups = { ValidationUserAccount.class, ValidationServiceAccount.class })
    @Size(min = 1, message = "Please provide an email address.", groups = { ValidationUserAccount.class, ValidationServiceAccount.class })
    private String email;

    @Column(name = "password")
    @Size(min = 6, message = "Your password must have at least 6 characters.", groups = { ValidationUserAccount.class })
    @NotNull(message = "Please provide your password.", groups = { ValidationUserAccount.class })
    @JsonIgnore
    @org.springframework.data.annotation.Transient
    private String password;

    /**
     * Exact moment when this user was created, or null if unknown.
     */
    @CreatedDate
    @JsonIgnore
    private Timestamp createdAt;

    /**
     * Last moment when this user profile was updated, or null if unknown.
     */
    @LastModifiedDate
    @JsonIgnore
    private Timestamp modifiedAt;

    @Column(name = "enabled", nullable = false)
    @JsonIgnore
    private boolean enabled;

    /**
     * Exact moment when the user account was enabled.
     */
    @Column(name = "enabled_at")
    @JsonIgnore
    private Timestamp enabledAt;

    @ManyToMany
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
    private final Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private final Set<AccessToken> accessTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private final Set<VerificationToken> verificationTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private final Set<PasswordResetToken> passwordResetTokens = new HashSet<>();

    @Valid
    @Embedded
    @JsonUnwrapped
    private Profile profile;

    @Transient
    private String origin;

    @Transient
    private URI originUrl;

    /**
     * Ensure that the path of the URL is effectively stripped from any trailing slashes.
     */
    @SneakyThrows
    public void setOriginUrl( URI originUrl ) {
        this.originUrl = new URI( originUrl.getScheme(), originUrl.getAuthority(), StringUtils.trimTrailingCharacter( originUrl.getPath(), '/' ), null, null );
    }

    /* Research related information */

    // @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @Lob
    @ElementCollection
    @CollectionTable(name = "descriptions", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyJoinColumn(name = "taxon_id")
    @Column(name = "description", columnDefinition = "TEXT")
    @JsonIgnore
    private final Map<Taxon, String> taxonDescriptions = new HashMap<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<UserTerm> userTerms = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @MapKey(name = "geneId")
    private final Map<Integer, UserGene> userGenes = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "user")
    @MapKey(name = "uberonId")
    private final Map<String, UserOrgan> userOrgans = new HashMap<>();

    /**
     * Associated terms to the user profile.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<UserOntologyTerm> userOntologyTerms = new HashSet<>();

    @Transient
    @JsonIgnore
    public Set<UserOntologyTerm> getUserOntologyTermsByOntology( Ontology ontology ) {
        return userOntologyTerms.stream().filter( uo -> uo.getOntology().equals( ontology ) ).collect( Collectors.toSet() );
    }

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
        return this.getUserGenes().values().stream()
                .map( UserGene::getTaxon )
                .sorted( Taxon.getComparator() )
                .collect( Collectors.toCollection( LinkedHashSet::new ) );
    }

    /**
     * Obtain a verified contact email for the user if available.
     */
    @JsonIgnore
    @Transient
    public Optional<InternetAddress> getVerifiedContactEmail() {
        try {
            if ( profile.isContactEmailVerified() ) {
                return Optional.of( new InternetAddress( profile.getContactEmail(), profile.getFullName() ) );
            } else if ( enabled ) {
                return Optional.of( new InternetAddress( email, profile.getFullName() ) );
            } else {
                return Optional.empty();
            }
        } catch ( UnsupportedEncodingException e ) {
            log.error( MessageFormat.format( "Could not encode a verified internet address for {0}.", this ) );
            return Optional.empty();
        }
    }

    @JsonIgnore
    @Transient
    public Optional<Timestamp> getVerifiedAtContactEmail() {
        if ( profile.isContactEmailVerified() ) {
            return Optional.ofNullable( profile.getContactEmailVerifiedAt() );
        } else if ( enabled ) {
            return Optional.ofNullable( enabledAt );
        } else {
            return Optional.empty();
        }
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
