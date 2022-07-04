package ubc.pavlab.rdp.model.ontology;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

/**
 * Represents an ontology term in a category.
 *
 * @author poirigui
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "ontology", "termId" })
@ToString(of = { "ontology", "termId", "name" })
@SuperBuilder
public abstract class OntologyTerm {

    /**
     * Ontology to which is term is part of.
     */
    @NaturalId
    @ManyToOne(optional = false)
    @JoinColumn(name = "ontology_id", nullable = false)
    private Ontology ontology;

    /**
     * Term ID as it appears in the ontology.
     */
    @NaturalId
    @Column(name = "term_id", nullable = false)
    private String termId;

    @Column(nullable = false)
    private String name;
}
