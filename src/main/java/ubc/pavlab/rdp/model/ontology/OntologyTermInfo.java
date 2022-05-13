package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;

/**
 * Unassociated ontology term.
 * <p>
 * If you need a user-associated term, see {@link UserOntologyTerm}.
 * <p>
 * This class implements {@link Comparable} so that it can be held {@link Ontology#getTerms()}'s sorted set.
 *
 * @author poirigui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class OntologyTermInfo extends OntologyTerm implements Comparable<OntologyTermInfo> {

    /**
     *
     */
    public static Comparator<OntologyTermInfo> getComparator() {
        return Comparator.comparing( OntologyTermInfo::getOntology, Ontology.getComparator() )
                .thenComparing( OntologyTermInfo::getOrder, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( OntologyTermInfo::getName );
    }

    /**
     * Indicate if the term is active.
     */
    @JsonIgnore
    @Column
    private boolean active;

    /**
     * Relative order of terms when sorted as per {@link #getComparator}.
     * <p>
     * Note that the term category has priority.
     */
    @JsonIgnore
    @Column
    private Integer order;

    /**
     * Indicate if the term has an icon and should be displayed.
     */
    @JsonIgnore
    @Transient
    private boolean hasIcon;

    private boolean isGroup;

    @Singular
    private SortedSet<OntologyTermInfo> subTerms;

    @Override
    public int compareTo( OntologyTermInfo ontologyTermInfo ) {
        return getComparator().compare( this, ontologyTermInfo );
    }
}
