package ubc.pavlab.rdp.model.ontology;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import ubc.pavlab.rdp.model.User;

import javax.persistence.Column;

/**
 * User-associated ontology term.
 *
 * @author poirigui
 */
@Data
@EqualsAndHashCode(of = { "user" }, callSuper = true)
@SuperBuilder
public class UserOntologyTerm extends OntologyTerm {

    /**
     * Create a new user term from a given term information and user.
     */
    public static UserOntologyTerm fromOntologyTermInfo( User user, OntologyTermInfo term ) {
        return UserOntologyTerm.builder()
                .user( user )
                .termInfo( term )
                .ontology( term.getOntology() )
                .definition( term.getDefinition() )
                .build();
    }

    /**
     * Original term from which this user term derived, if still available otherwise null.
     */
    @Column
    private OntologyTermInfo termInfo;

    /**
     * User to whome the term belong.
     */
    @Column(nullable = false)
    private User user;
}
