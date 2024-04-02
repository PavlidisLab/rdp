package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.lang.Nullable;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.model.ontology.Ontology;
import ubc.pavlab.rdp.model.ontology.UserOntologyTerm;

import javax.mail.internet.InternetAddress;
import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
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
public class User implements RemoteResource, UserContent, Serializable {

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

    public static UserBuilder builder( Profile profile ) {
        return new UserBuilder().profile( profile );
    }

    public static Comparator<User> getComparator() {
        return Comparator
                .comparing( ( User u ) -> u.getProfile().getFullName() )
                .thenComparing( User::getOriginUrl, Comparator.nullsLast( Comparator.naturalOrder() ) )
                // at least one of the two must be non-null
                .thenComparing( User::getId, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( User::getAnonymousId, Comparator.nullsLast( Comparator.naturalOrder() ) );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;

    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID anonymousId;

    /**
     * For the JSON serialization, the representation is given by {@link #getVerifiedContactEmailJsonValue()}.
     */
    @NaturalId
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "email", unique = true, nullable = false)
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
    private Instant createdAt;

    /**
     * Last moment when this user profile was updated, or null if unknown.
     */
    @LastModifiedDate
    @JsonIgnore
    private Instant modifiedAt;

    @JsonIgnore
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /**
     * Exact moment when the user account was enabled.
     */
    @Column(name = "enabled_at")
    @JsonIgnore
    private Instant enabledAt;

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
    @NotNull
    @Embedded
    @JsonUnwrapped
    private Profile profile;

    @Transient
    private String origin;

    @Transient
    private URI originUrl;

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
    public boolean hasTaxon( Taxon taxon ) {
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

    /**
     * This is meant for JSON serialization of the user's public-facing email.
     */
    @Nullable
    @JsonProperty("email")
    public String getVerifiedContactEmailJsonValue() {
        if ( profile != null && profile.isContactEmailVerified() ) {
            return profile.getContactEmail();
        } else if ( enabled ) {
            return email;
        } else {
            return null;
        }
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    @Transient
    public Optional<Instant> getVerifiedAtContactEmail() {
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
    @JsonProperty(value = "privacyLevel")
    public PrivacyLevelType getEffectivePrivacyLevel() {
        // this is a fallback
        if ( getProfile() == null || getProfile().getPrivacyLevel() == null ) {
            log.warn( MessageFormat.format( "User {0} has no profile or a null privacy level defined in its profile.", this ) );
            return PrivacyLevelType.PRIVATE;
        }
        return getProfile().getPrivacyLevel();
    }
}
