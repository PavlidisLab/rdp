package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"geneId"})
@ToString(of = {"geneId", "symbol", "taxon"})
public class Gene {

    @Column(name = "gene_id")
    private int geneId;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "symbol", length = 63)
    private String symbol;

    @Column(name = "description", columnDefinition = "TEXT")
    private String name;

    @Column(name = "synonyms", columnDefinition = "TEXT")
    private String aliases;

    @Temporal(TemporalType.DATE)
    @Column(name = "modification_date")
    private Date modificationDate;

    @Transient
    @JsonIgnore
    private Set<GeneOntologyTerm> terms = new HashSet<>();

    public Collection<GeneOntologyTerm> getAllTerms( boolean includePartOf, boolean propagateUpwards ) {

        Collection<GeneOntologyTerm> allGOTermSet = new HashSet<>();

        for ( GeneOntologyTerm term : terms ) {
            allGOTermSet.add( term );

            if ( propagateUpwards ) {
                allGOTermSet.addAll( term.getAncestors( includePartOf ) );
            }
        }

        return Collections.unmodifiableCollection( allGOTermSet );
    }
}
