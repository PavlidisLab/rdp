package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import javax.persistence.*;
import java.net.URL;
import java.util.Comparator;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An ontology of terms.
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
public class Ontology implements Comparable<Ontology> {

    /**
     * Maximum length for an ontology name.
     * TODO: retrieve this from the metamodel
     */
    public static final int MAX_NAME_LENGTH = 255;

    public static OntologyBuilder builder( @NonNull String name ) {
        return new OntologyBuilder().name( name );
    }

    /**
     * Obtain a comparator for ordering categories.
     * <p>
     * Use the {@link #getOrdering()} if available, otherwise rely on the {@link #getName()}.
     */
    public static Comparator<Ontology> getComparator() {
        return Comparator.comparing( Ontology::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( Ontology::isActive, Comparator.reverseOrder() )
                .thenComparing( Ontology::getName );
    }

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ontology_id")
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String definition;

    /**
     * For the public API.
     * <p>
     * If you need to get the number of terms, use {@link #getTerms()} with 'size', it's very efficient!
     */
    @Transient
    private Long numberOfTerms;

    @Transient
    private Long numberOfObsoleteTerms;

    /**
     * Set of terms associated to this category.
     * <p>
     * The {@link LazyCollectionOption#EXTRA} makes it such that calling terms.size() or terms.contains() does not
     * result in a full initialization.
     * <p>
     * Be extremely careful with this collection.
     * <p>
     * Note: I know it's tempting to map this by term ID with {@link MapKey}, but don't. This collection can get pretty
     * huge.
     */
    @JsonIgnore
    @OneToMany(mappedBy = "ontology", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordering asc, name asc, termId asc")
    @LazyCollection(LazyCollectionOption.EXTRA)
    private final SortedSet<OntologyTermInfo> terms = new TreeSet<>();

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
     * Indicate of the ontology is available for searching by gene.
     */
    @JsonIgnore
    private boolean availableForGeneSearch;

    /**
     * Relative order of categories when sorted as per {@link #getComparator()}.
     */
    @JsonIgnore
    private Integer ordering;

    @Override
    public int compareTo( Ontology ontology ) {
        return getComparator().compare( this, ontology );
    }

    /**
     * Obtain a resolvable for the title of this ontology.
     * <p>
     * The default value is the upper case {@link #name}.
     */
    @JsonIgnore
    public DefaultMessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies." + Ontology.this.getName() + ".title" }, name.toUpperCase() );
    }

    /**
     * Obtain a resolvable for the definition of this ontology.
     * <p>
     * Since the {@link #definition} is nullable, you should always check for a {@link org.springframework.context.NoSuchMessageException}
     * when calling {@link org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, Locale)}.
     */
    @JsonIgnore
    public DefaultMessageSourceResolvable getResolvableDefinition() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies." + Ontology.this.getName() + ".definition" }, definition );
    }
}
