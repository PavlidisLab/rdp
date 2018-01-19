package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "go_term")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"geneOntologyId"})
@ToString
public class GeneOntologyTerm {

    public enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;

    @Column(name = "go_id")
    private String geneOntologyId;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "frequency")
    private int frequency;

    @Column(name = "size")
    private int size;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "aspect")
    private GOAspect aspect;
}
