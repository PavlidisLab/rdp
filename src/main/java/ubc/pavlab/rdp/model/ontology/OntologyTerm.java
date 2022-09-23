package ubc.pavlab.rdp.model.ontology;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.NaturalId;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Locale;

/**
 * Represents an ontology term in a category.
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
public abstract class OntologyTerm implements Serializable {

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

    /**
     * Name of this term.
     * <p>
     * This should be the value of the <code>name</code> entry in the OBO <code>[Term]</code> stanza.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Obtain a resolvable for the title of this term.
     * <p>
     * The default value is the {@link #name}.
     */
    @JsonIgnore
    public DefaultMessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies." + getOntology().getName() + ".terms." + getName() + ".title" }, name );
    }

    /**
     * Obtain a resolvable for the definition of this term.
     * <p>
     * There is no default message, so you should always check for a potential {@link org.springframework.context.NoSuchMessageException}
     * when calling {@link org.springframework.context.MessageSource#getMessage(MessageSourceResolvable, Locale)}.
     */
    @JsonIgnore
    public DefaultMessageSourceResolvable getResolvableDefinition() {
        return new DefaultMessageSourceResolvable( new String[]{ "rdp.ontologies." + getOntology().getName() + ".terms." + getName() + ".definition" } );
    }
}
