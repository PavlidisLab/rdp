package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Comparator;
import java.util.SortedSet;

/**
 * An ontology of terms.
 * <p>
 * TODO: mimic the structure of a OWL record.
 *
 * @author poirigui
 */
@Data
@EqualsAndHashCode(of = { "id" })
@Builder
public class Ontology {

    /**
     * Obtain a comparator for ordering categories.
     * <p>
     * Use the {@link #getOrder()} if available, otherwise rely on the {@link #getName()}.
     */
    public static Comparator<Ontology> getComparator() {
        return Comparator.comparing( Ontology::getOrder, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( Ontology::getName );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column
    private String name;

    /**
     * Set of terms associated to this category.
     */
    private SortedSet<OntologyTermInfo> terms;

    /**
     * Indicate if the terms in this category are expected to contain icons.
     */
    @JsonIgnore
    @Transient
    public boolean getTermsHaveIcons() {
        return terms.stream()
                .anyMatch( OntologyTermInfo::isHasIcon );
    }

    @JsonIgnore
    private boolean active;

    /**
     * Relative order of categories when sorted as per {@link #getComparator()}.
     */
    @JsonIgnore
    private Integer order;
}
