package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Transient;
import ubc.pavlab.rdp.listeners.UserEntityListener;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@Table(name = "user")
@EntityListeners(UserEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString( of = {"id", "email", "enabled"})
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "user_id")
    @JsonIgnore
	private int id;

	@Column(name = "email")
	@Email(message = "*Please provide a valid Email")
	@NotEmpty(message = "*Please provide an email")
	private String email;

	@Column(name = "password")
	@Length(min = 6, message = "*Your password must have at least 6 characters")
	@NotEmpty(message = "*Please provide your password")
    @JsonIgnore
	@Transient
	private String password;

	@Column(name = "enabled")
    @JsonIgnore
	private boolean enabled;

	@ManyToMany
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
	private Set<Role> roles = new HashSet<>();

	@Embedded
    @JsonUnwrapped
    private Profile profile;

	/* Research related information */
    @ElementCollection
    @CollectionTable(name = "descriptions", joinColumns = @JoinColumn(name = "user_id"))
	@MapKeyJoinColumn(name="taxon_id")
    @Column(name = "description", columnDefinition = "TEXT")
    private Map<Taxon, String> taxonDescriptions = new HashMap<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id")
	private Set<UserTerm> userTerms = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "user_id")
	private Set<UserGene> userGenes = new HashSet<>();

    @JsonIgnore
    @Transient
    public Set<Gene> getGenesByTaxon( Taxon taxon ) {
        return this.getUserGenes().stream().filter( gene -> gene.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<Gene> getGenesByTaxonAndTier( Taxon taxon, Set<TierType> tiers) {
        return this.getUserGenes().stream()
                .filter( gene -> gene.getTaxon().equals( taxon ) && tiers.contains( gene.getTier()) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<UserTerm> getTermsByTaxon( Taxon taxon ) {
        return this.getUserTerms().stream().filter( term -> term.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }
}
