package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * GO term tracked by a user.
 *
 * TODO: add user to {@link EqualsAndHashCode} definition to distinguish
 * between terms from different users.
 *
 * Created by mjacobson on 28/01/18.
 */
@Entity
@Table(name = "term",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "taxon_id", "go_id" }) },
        indexes = { @Index(columnList = "go_id", name = "go_id_hidx") }
)
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@EqualsAndHashCode(of = { "user", "taxon" }, callSuper = true)
@ToString(of = {"user", "taxon"}, callSuper = true)
@NoArgsConstructor
public class UserTerm extends GeneOntologyTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    /**
     * Number of overlap with the user TIER1 and TIER2 genes.
     */
    @Column(name = "frequency")
    private int frequency;

    @Column(name = "size")
    private int size;

    public static UserTerm createUserTerm( User user, GeneOntologyTerm term, Taxon taxon ) {
        UserTerm userTerm = new UserTerm();
        userTerm.setGoId( term.getGoId() );
        userTerm.updateTerm( term );
        userTerm.user = user;
        userTerm.taxon = taxon;
        userTerm.size = userTerm.getSize( taxon );
        return userTerm;
    }

    public void updateTerm( GeneOntologyTerm term ) {
        this.setName( term.getName() );
        this.setDefinition( term.getDefinition() );
        this.setAspect( term.getAspect() );
        this.setObsolete( term.isObsolete() );

        this.setParents( term.getParents() );
        this.setChildren( term.getChildren() );
        this.setSizesByTaxonId( term.getSizesByTaxonId() );
        this.setDirectGeneIds( term.getDirectGeneIds() );
    }

}
