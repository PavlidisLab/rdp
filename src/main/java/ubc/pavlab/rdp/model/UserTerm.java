package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

/**
 * Represents an association between a user and a GO term.
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
@EqualsAndHashCode(of = { "user", "taxon", "goId" })
@NoArgsConstructor
public class UserTerm extends GeneOntologyTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "frequency")
    private int frequency;

    @Column(name = "size")
    private int size;

    public static UserTerm createUserTerm( GeneOntologyTerm term, Taxon taxon, Set<? extends Gene> overlapGenes ) {
        UserTerm userTerm = new UserTerm();
        userTerm.updateTerm( term );
        userTerm.taxon = taxon;

        userTerm.size = userTerm.getSize( taxon );
        if ( overlapGenes != null ) {
            userTerm.frequency = userTerm.computeOverlapFrequency( overlapGenes );
        }
        return userTerm;
    }

    public void updateTerm( GeneOntologyTerm term ) {
        this.setGoId( term.getGoId() );
        this.setName( term.getName() );
        this.setDefinition( term.getDefinition() );
        this.setAspect( term.getAspect() );
        this.setObsolete( term.isObsolete() );

        this.setParents( term.getParents() );
        this.setChildren( term.getChildren() );
        this.setSizesByTaxon( term.getSizesByTaxon() );
        this.setDirectGenes( term.getDirectGenes() );
    }

    private Integer computeOverlapFrequency( Set<? extends Gene> genes ) {
        Integer frequency = 0;
        for ( Gene g : genes ) {
            Collection<GeneOntologyTerm> directTerms = g.getAllTerms( true, true );

            for ( GeneOntologyTerm term : directTerms ) {
                if ( term.equals( this ) ) {
                    frequency++;
                    // Can break because a gene cannot (and shouldn't) have duplicate terms
                    break;
                }
            }

        }
        return frequency;
    }

}
