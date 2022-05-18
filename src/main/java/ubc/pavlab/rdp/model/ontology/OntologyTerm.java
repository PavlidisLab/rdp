package ubc.pavlab.rdp.model.ontology;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;
import ubc.pavlab.rdp.model.Taxon;

import javax.persistence.*;

/**
 * Represents an ontology term in a category.
 * <p>
 * TODO: mimic the structure of a OWL [Term] record.
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
