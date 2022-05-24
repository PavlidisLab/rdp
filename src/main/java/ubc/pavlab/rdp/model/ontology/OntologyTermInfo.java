package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Unassociated ontology term.
 * <p>
 * If you need a user-associated term, see {@link UserOntologyTerm}.
 * <p>
 * This class implements {@link Comparable} so that it can be held {@link Ontology#getTerms()}'s sorted set.
 *
 * @author poirigui
 */

@Entity
@Table(name = "ontology_term_info",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "ontology_id", "term_id" }) },
        indexes = { @Index(columnList = "name") })
@Getter
@Setter
@NoArgsConstructor
@ToString(of = { "id" }, callSuper = true)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@SuperBuilder
public class OntologyTermInfo extends OntologyTerm implements Serializable, Comparable<OntologyTermInfo> {

    /**
     * Maximum size of
     * TODO: gather this information from the metamodel.
     */
    public static final int MAX_SYNONYM_LENGTH = 255;

    public static OntologyTermInfoBuilder<?, ?> builder( @NonNull Ontology ontology, @NonNull String termId ) {
        return new OntologyTermInfo.OntologyTermInfoBuilderImpl()
                .ontology( ontology )
                .termId( termId );
    }

    /**
     *
     */
    public static Comparator<OntologyTermInfo> getComparator() {
        return Comparator.comparing( OntologyTermInfo::getOntology, Comparator.nullsLast( Ontology.getComparator() ) )
                .thenComparing( OntologyTermInfo::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( OntologyTermInfo::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( OntologyTermInfo::getTermId );
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ontology_term_info_id")
    private Integer id;

    @ElementCollection
    @CollectionTable(name = "ontology_term_info_alt_ids", joinColumns = { @JoinColumn(name = "ontology_term_info_id") })
    @Column(name = "alt_id", nullable = false)
    private final Set<String> altTermIds = new HashSet<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String definition;

    @ElementCollection
    @CollectionTable(name = "ontology_term_info_synonyms", joinColumns = { @JoinColumn(name = "ontology_term_info_id") })
    @Column(name = "synonym", nullable = false)
    private final Set<String> synonyms = new HashSet<>();

    /**
     * Indicate if the term is obsolete.
     */
    @JsonIgnore
    @Column(nullable = false)
    private boolean obsolete;

    /**
     * Indicate if the term is active.
     */
    @JsonIgnore
    @Column(nullable = false)
    private boolean active;

    /**
     * Relative order of terms when sorted as per {@link #getComparator}.
     * <p>
     * Note that the term category has priority.
     */
    @JsonIgnore
    @Column
    private Integer ordering;

    /**
     * Indicate if the term has an icon and should be displayed.
     */
    @JsonIgnore
    @Column(nullable = false)
    private boolean hasIcon;

    /**
     * Indicate if the term is merely a grouping of other terms, in which case it will be displayed but it won't be
     * selectable and thus convertible into a {@link UserOntologyTerm}.
     * <p>
     * TODO: this can be renamed
     */
    @JsonIgnore
    @Column(nullable = false)
    private boolean isGroup;

    /**
     * An inverse mapping of {@link #subTerms} to walk up the "is-a" class hierarchy.
     * <p>
     * If empty, the term has no parents and can be considered as a root term in the ontology.
     */
    @Builder.Default
    @ManyToMany(mappedBy = "subTerms")
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @JsonIgnore
    private final Set<OntologyTermInfo> superTerms = new HashSet<>();

    /**
     * Collection of terms that satisfy the "is-a" subclassing.
     * <p>
     * Terms are ordered as per {@link OntologyTermInfo#getComparator()} although the actual ordering is performed by
     * Hibernate via an {@link OrderBy} annotation.
     * <p>
     * We only cascade when the term is initially persisted to make save() operation simpler. This means that you can
     * create a full term structure without having to attach them the ontology.
     * <p>
     * Cascaded removal only occurs when the ontology itself is removed. See {@link Ontology#getTerms()} for the full
     * details.
     */
    @Singular
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "ontology_term_info_sub_terms",
            joinColumns = @JoinColumn(name = "ontology_term_info_id"),
            inverseJoinColumns = @JoinColumn(name = "ontology_sub_term_info_id"))
    @OrderBy("ordering asc, name asc, termId asc")
    @JsonIgnore
    private SortedSet<OntologyTermInfo> subTerms = new TreeSet<>();

    @Override
    public int compareTo( OntologyTermInfo ontologyTermInfo ) {
        return getComparator().compare( this, ontologyTermInfo );
    }
}
