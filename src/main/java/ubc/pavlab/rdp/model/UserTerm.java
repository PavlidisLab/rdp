package ubc.pavlab.rdp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

/**
 * Created by mjacobson on 28/01/18.
 */
@Entity
@Table(name = "term")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
public class UserTerm extends GeneOntologyTerm {

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "frequency")
    private int frequency;

    @Column(name = "size")
    private int size;

    public void updateTerm(GeneOntologyTerm term) {
        this.setId( term.getId() );
        this.setName( term.getName() );
        this.setDefinition( term.getDefinition() );
        this.setAspect( term.getAspect() );
        this.setObsolete( term.isObsolete() );

        this.setParents( term.getParents() );
        this.setChildren( term.getChildren() );
        this.setSizesByTaxon( term.getSizesByTaxon() );
        this.setDirectGenes( term.getDirectGenes() );
    }

    public UserTerm(GeneOntologyTerm term, Taxon taxon, Set<Gene> overlapGenes) {
        super();
        this.updateTerm( term );
        this.taxon = taxon;

        this.size = this.getSize( taxon );
        if (overlapGenes != null) {
            this.frequency = computeOverlapFrequency( overlapGenes );
        }
    }

    private Integer computeOverlapFrequency( Set<Gene> genes ) {
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
