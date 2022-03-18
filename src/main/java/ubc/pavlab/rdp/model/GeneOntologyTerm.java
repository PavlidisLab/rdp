package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.NaturalId;
import ubc.pavlab.rdp.model.enums.Aspect;
import ubc.pavlab.rdp.model.enums.RelationshipType;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 17/01/18.
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = { "goId" })
@ToString(of = { "goId", "name" })
public abstract class GeneOntologyTerm {

    @NaturalId
    @Column(name = "go_id", length = 10)
    private String goId;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "aspect")
    private Aspect aspect;
}
