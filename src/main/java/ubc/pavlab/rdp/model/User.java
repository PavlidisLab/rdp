package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.Transient;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString( of = {"id", "email", "active"})
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

	@Column(name = "active")
    @JsonIgnore
	private int active;

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @JsonIgnore
	private Set<Role> roles = new HashSet<>();

	@Embedded
    @JsonUnwrapped
    private Profile profile;

	/* Research related information */

	@ManyToMany(cascade = CascadeType.ALL)
	@JoinTable(name = "user_taxon", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "taxon_id"))
	private Set<Taxon> taxons = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "user_id")
	private Set<TaxonDescription> taxonDescriptions = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id")
	private Set<GeneOntologyTerm> goTerms = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "pk.user", orphanRemoval = true)
	private Set<UserGene> geneAssociations = new HashSet<>();

    @JsonIgnore
    @Transient
    public Set<Gene> getGenes() {
        return this.getGeneAssociations().stream().map( UserGene::getGene ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<Gene> getGenesByTaxon( Taxon taxon ) {
        return this.getGeneAssociations().stream().map( UserGene::getGene )
                .filter( gene -> gene.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<Gene> getGenesByTaxonAndTier( Taxon taxon, Set<UserGene.TierType> tiers) {
        return this.getGeneAssociations().stream()
                .filter( ga -> ga.getGene().getTaxon().equals( taxon ) && tiers.contains( ga.getTier()) )
                .map( UserGene::getGene ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<UserGene> getGenesAssociationsByTaxon( Taxon taxon ) {
        return this.getGeneAssociations().stream().filter( ga -> ga.getGene().getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }

    @JsonIgnore
    @Transient
    public Set<GeneOntologyTerm> getTermsByTaxon( Taxon taxon ) {
        return this.getGoTerms().stream().filter( term -> term.getTaxon().equals( taxon ) ).collect( Collectors.toSet() );
    }
}
