package ubc.pavlab.rdp.model.ontology;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;
import ubc.pavlab.rdp.model.User;
import ubc.pavlab.rdp.model.UserContent;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.util.Optional;

/**
 * User-associated ontology term.
 *
 * @author poirigui
 */
@Entity
@Table(name = "user_ontology_term",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "ontology_id", "term_id", "user_id" }) })
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "user" }, callSuper = true)
@ToString(of = { "user" }, callSuper = true)
@SuperBuilder
public class UserOntologyTerm extends OntologyTerm implements UserContent {

    public static UserOntologyTermBuilder<?, ?> builder( User user, Ontology ontology, String termId ) {
        return new UserOntologyTermBuilderImpl()
                .user( user )
                .ontology( ontology )
                .termId( termId );
    }

    /**
     * Create a new user term from a given term information and user.
     */
    public static UserOntologyTerm fromOntologyTermInfo( User user, OntologyTermInfo term ) {
        if ( term.isGroup() ) {
            throw new IllegalArgumentException( String.format( "Grouping term %s cannot be converted to user term.", term ) );
        }
        return UserOntologyTerm.builder( user, term.getOntology(), term.getTermId() )
                .name( term.getName() )
                .termInfo( term )
                .build();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_ontology_term_id")
    private Integer id;

    /**
     * Original term from which this user term derived, if still available otherwise null.
     */
    @ManyToOne
    @JoinColumns(value = {
            @JoinColumn(name = "ontology_id", referencedColumnName = "ontology_id", insertable = false, updatable = false),
            @JoinColumn(name = "term_id", referencedColumnName = "term_id", insertable = false, updatable = false) })
    private OntologyTermInfo termInfo;

    /**
     * User to whome the term belong.
     */
    @NaturalId
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Override
    public Optional<User> getOwner() {
        return Optional.of( user );
    }

    @Override
    public @NonNull PrivacyLevelType getEffectivePrivacyLevel() {
        return user.getEffectivePrivacyLevel();
    }
}
