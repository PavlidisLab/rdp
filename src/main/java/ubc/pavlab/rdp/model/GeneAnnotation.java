package ubc.pavlab.rdp.model;

import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene_annotation")
@AssociationOverrides({ @AssociationOverride(name = "pk.goId", joinColumns = @JoinColumn(name = "go_id")),
        @AssociationOverride(name = "pk.gene", joinColumns = @JoinColumn(name = "gene_id")) })
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"pk"})
@ToString
public class GeneAnnotation {

    @EmbeddedId
    private GeneAnnotationId pk = new GeneAnnotationId();

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

    @Transient
    public Gene getGene() {
        return this.pk.getGene();
    }

    @Transient
    public String getGeneOntologyId() {
        return this.pk.getGoId();
    }

}

