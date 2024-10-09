package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.URL;
import org.springframework.lang.Nullable;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 18/01/18.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Profile {

    @Column(name = "name")
    @NotNull(message = "Please provide your name.", groups = { User.ValidationUserAccount.class, User.ValidationServiceAccount.class })
    @Size(min = 1, message = "Please provide your name.", groups = { User.ValidationUserAccount.class, User.ValidationServiceAccount.class })
    private String name;

    @Column(name = "last_name")
    @NotNull(message = "Please provide your last name.", groups = { User.ValidationUserAccount.class })
    @Size(min = 1, message = "Please provide your last name.", groups = { User.ValidationUserAccount.class })
    private String lastName;

    @Transient
    @JsonIgnore
    public String getFullName() {
        if ( lastName == null || lastName.isEmpty() ) {
            return name == null ? "" : name;
        } else if ( name == null || name.isEmpty() ) {
            return "";
        } else {
            return MessageFormat.format( "{0}, {1}", lastName, name );
        }
    }

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    @Nullable
    private String description;

    @Column(name = "organization")
    private String organization;

    @Column(name = "department")
    private String department;

    @Column(name = "phone")
    private String phone;

    @Nullable
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Email(message = "Your email address is not valid.")
    @Column(name = "contact_email")
    private String contactEmail;

    @JsonIgnore
    @Column(name = "contact_email_verified", nullable = false)
    private boolean contactEmailVerified;

    @JsonIgnore
    @Nullable
    @Column(name = "contact_email_verified_at")
    private Instant contactEmailVerifiedAt;

    @Column(name = "website")
    @URL
    private String website;

    @Nullable
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "privacy_level")
    private PrivacyLevelType privacyLevel;

    @Schema(hidden = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "shared", nullable = false)
    private boolean shared;

    @Schema(hidden = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "hide_genelist", nullable = false)
    private boolean hideGenelist;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private final Set<Publication> publications = new HashSet<>();

    @Nullable
    @Enumerated(EnumType.STRING)
    @Column(name = "researcher_position")
    private ResearcherPosition researcherPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "researcher_category", nullable = false)
    @ElementCollection
    @CollectionTable(name = "user_researcher_category", joinColumns = { @JoinColumn(name = "user_id") })
    private final Set<ResearcherCategory> researcherCategories = new HashSet<>();
}
