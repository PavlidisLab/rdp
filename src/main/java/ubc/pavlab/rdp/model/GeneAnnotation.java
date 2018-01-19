package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene_annotation")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"goId", "gene"})
@ToString
public class GeneAnnotation {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;

    @Column(name = "go_id")
    private String goId;

    @ManyToOne
    @JoinColumn(name = "gene_id")
    private Gene gene;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column(name = "evidence", length = 64)
    private String evidence;

    @Column(name = "qualifier", length = 64)
    private String qualifier;

    @Column(name = "term", columnDefinition = "TEXT")
    private String geneOntologyTerm;

    @Column(name = "pmid", columnDefinition = "TEXT")
    private String pubMed;

    @Column(name = "category", length = 64)
    private String category;

}

