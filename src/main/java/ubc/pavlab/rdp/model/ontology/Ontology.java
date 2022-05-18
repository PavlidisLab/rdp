package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.net.URL;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An ontology of terms.
 * <p>
 * TODO: mimic the structure of a OWL record.
 *
 * @author poirigui
 */
@Entity
@Table(name = "ontology")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(of = { "name" })
@ToString(of = { "id", "name" })
public class Ontology {

    /**
     * Obtain a comparator for ordering categories.
     * <p>
     * Use the {@link #getOrdering()} if available, otherwise rely on the {@link #getName()}.
     */
    public static Comparator<Ontology> getComparator() {
        return Comparator.comparing( Ontology::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( Ontology::getName );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ontology_id")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    /**
     * Set of terms associated to this category.
     */
    @Singular
    @OneToMany(mappedBy = "ontology", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering asc, name asc")
    @JsonIgnore
    private SortedSet<OntologyTermInfo> terms = new TreeSet<>();

    /**
     * Indicate if the terms in this category are expected to contain icons.
     */
    @JsonIgnore
    @Transient
    public boolean getTermsHaveIcons() {
        return terms.stream()
                .anyMatch( OntologyTermInfo::isHasIcon );
    }

    /**
     * URL used to resolve the ontology source.
     * <p>
     * The only supported format for now is OBO.
     */
    @JsonIgnore
    private URL ontologyUrl;

    /**
     * Indicate if the ontology is active.
     */
    @JsonIgnore
    private boolean active;

    /**
     * Relative order of categories when sorted as per {@link #getComparator()}.
     */
    @JsonIgnore
    private Integer ordering;
}
