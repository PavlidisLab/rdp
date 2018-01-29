package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mjacobson on 17/01/18.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class Gene {

    @Column(name = "gene_id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "description", columnDefinition = "TEXT")
    private String name;

    @Column(name = "synonyms", columnDefinition = "TEXT")
    private String aliases;

    @Column(name = "modification_date")
    private int modificationDate;

    @Transient
    @JsonIgnore
    private Set<GeneOntologyTerm> terms = new HashSet<>();
}
