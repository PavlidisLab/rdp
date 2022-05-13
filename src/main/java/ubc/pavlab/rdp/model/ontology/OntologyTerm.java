package ubc.pavlab.rdp.model.ontology;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ubc.pavlab.rdp.model.Taxon;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Represents an ontology term in a category.
 * <p>
 * TODO: mimic the structure of a OWL [Term] record.
 *
 * @author poirigui
 */
@Data
@EqualsAndHashCode(of = { "id", "ontology", "taxon" })
@SuperBuilder
public abstract class OntologyTerm {

    @Id
    private String id;

    @ManyToOne
    private Ontology ontology;

    @Column
    private Taxon taxon;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String definition;
}
