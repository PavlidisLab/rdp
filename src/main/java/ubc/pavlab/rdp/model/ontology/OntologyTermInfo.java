package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SortComparator;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import javax.persistence.*;
import java.text.Collator;
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
public class OntologyTermInfo extends OntologyTerm implements Comparable<OntologyTermInfo> {

    /**
     * Maximum size of term ID and name.
     * <p>
     * TODO: gather this information from the metamodel.
     */
    public static final int MAX_TERM_ID_LENGTH = 255, MAX_NAME_LENGTH = 255;

    /**
     * Maximum size of a term synonym.
     * <p>
     * Unlike the limits defined above {@link #MAX_TERM_ID_LENGTH} and {@link #MAX_NAME_LENGTH}, this one is part of the
     * column definition.
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
                .thenComparing( OntologyTermInfo::isActive, Comparator.reverseOrder() )
                .thenComparing( OntologyTermInfo::getName, Comparator.nullsLast( Comparator.naturalOrder() ) )
                // this should never be null, but sometimes is due to Hibernate merge (see https://github.com/PavlidisLab/rdp/issues/186)
                .thenComparing( OntologyTermInfo::getTermId, Comparator.nullsLast( Comparator.naturalOrder() ) );
    }

    /**
     * Comparator used to compare and distinguish entries of {@link #synonyms}.
     * <p>
     * This is important because synonyms are part of the primary key alongside the {@link #id} and must respect the
     * database collation.
     */
    public static class SynonymComparator implements Comparator<String> {

        private static final Collator collator;

        static {
            collator = Collator.getInstance( Locale.ENGLISH );
            collator.setStrength( Collator.PRIMARY );
        }

        @Override
        public int compare( String a, String b ) {
            return collator.compare( a, b );
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ontology_term_info_id")
    @JsonIgnore
    private Integer id;

    @ElementCollection
    @CollectionTable(name = "ontology_term_info_alt_ids", joinColumns = { @JoinColumn(name = "ontology_term_info_id") })
    @Column(name = "alt_id", nullable = false)
    private final Set<String> altTermIds = new HashSet<>();

    @Lob
    @Column(columnDefinition = "TEXT")
    private String definition;

    /**
     * Synonyms of the term.
     * <p>
     * Note: the collation of the synonym has to be binary since it is part of the primary key. Otherwise, two strings
     * differing by their case (i.e. 'foo' and 'Foo') would conflict.
     */
    @SortComparator(SynonymComparator.class)
    @ElementCollection
    @CollectionTable(name = "ontology_term_info_synonyms", joinColumns = { @JoinColumn(name = "ontology_term_info_id") })
    @Column(name = "synonym", nullable = false)
    private final SortedSet<String> synonyms = new TreeSet<>( new SynonymComparator() );

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
     * TODO: this can be renamed to isGrouping
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

    /**
     * CollectiListon of term IDs of all active sub terms of this term.
     * <p>
     * Used for the public API.
     */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("subTerms")
    private Set<String> subTermIds;

    @Override
    public int compareTo( OntologyTermInfo ontologyTermInfo ) {
        return getComparator().compare( this, ontologyTermInfo );
    }

    /**
     * {@inheritDoc
     * <p>
     * Since the {@link #definition} is nullable, you should always check for a {@link org.springframework.context.NoSuchMessageException}
     * when calling {@link org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, Locale)}.
     */
    @Override
    @JsonIgnore
    public DefaultMessageSourceResolvable getResolvableDefinition() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies." + getOntology().getName() + ".terms." + getName() + ".definition" }, definition );
    }
}
