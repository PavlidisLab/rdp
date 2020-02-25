package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 18/01/18.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Profile {
    @Column(name = "name")
    @NotEmpty(message = "*Please provide your name")
    private String name;

    @Column(name = "last_name")
    @NotEmpty(message = "*Please provide your last name")
    private String lastName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "organization")
    private String organization;

    @Column(name = "department")
    private String department;

    @Column(name = "phone")
    private String phone;

    @Column(name = "website")
    @URL
    private String website;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "privacy_level")
    private PrivacyLevelType privacyLevel;

    @Column(name = "shared")
    private Boolean shared;

    @Column(name= "hide_genelist")
    private Boolean hideGenelist;

    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "user_id")
    private Set<Publication> publications = new HashSet<>();
}
