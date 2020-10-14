package ubc.pavlab.rdp.model;

import lombok.Data;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.ResearcherCategory;
import ubc.pavlab.rdp.model.enums.ResearcherPosition;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 18/01/18.
 */
@Data
@Embeddable
public class Profile {
    @Column(name = "name")
    @NotEmpty(message = "Please provide your name.")
    private String name;

    @Column(name = "last_name")
    @NotEmpty(message = "Please provide your last name.")
    private String lastName;

    @Transient
    public String getFullName() {
        return name + " " + lastName;
    }

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "organization")
    private String organization;

    @Column(name = "department")
    private String department;

    @Column(name = "phone")
    private String phone;

    @Email(message = "Your email address is not valid.")
    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "website")
    @URL
    private String website;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "privacy_level")
    private PrivacyLevelType privacyLevel;

    @Column(name = "shared")
    private Boolean shared;

    @Column(name = "hide_genelist")
    private Boolean hideGenelist;

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Set<Publication> publications = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "researcher_position")
    private ResearcherPosition researcherPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "researcher_category")
    @ElementCollection
    @CollectionTable(name = "user_researcher_category", joinColumns = { @JoinColumn(name = "user_id") })
    private Set<ResearcherCategory> researcherCategories = new HashSet<>();
}
