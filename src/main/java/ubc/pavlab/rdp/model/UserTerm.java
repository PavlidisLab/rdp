package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 28/01/18.
 */
@Entity
@Table(name = "go_term")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"taxon", "term"})
@ToString
public class UserTerm {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "frequency")
    private int frequency;

    @Column(name = "size")
    private int size;

    @Embedded
    @JsonUnwrapped
    private GeneOntologyTerm term;

}
