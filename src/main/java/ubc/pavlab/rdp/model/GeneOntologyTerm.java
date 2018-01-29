package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import ubc.pavlab.rdp.model.enums.Aspect;

import javax.persistence.*;
import java.util.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class GeneOntologyTerm {

    @Column(name = "go_id")
    private String id;

    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "definition", columnDefinition = "TEXT")
    private String definition;

    @Enumerated(EnumType.STRING)
    @Column(name = "aspect")
    private Aspect aspect;

    @Transient
    private boolean isObsolete;

    @JsonIgnore
    @Transient
    private Collection<Relationship> parents = new HashSet<>();

    @JsonIgnore
    @Transient
    private Collection<Relationship> children = new HashSet<>();

    @JsonIgnore
    @Transient
    private Map<Taxon, Long> sizesByTaxon = new HashMap<>();

    @JsonIgnore
    @Transient
    private Set<Gene> genes = new HashSet<>();

    public void addChild( Relationship child ) {
        this.children.add( child );
    }

    public void addParent( Relationship parent ) {
        this.parents.add( parent );
    }

    public Long getSize( Taxon taxon ) {
        Long res = this.sizesByTaxon.get( taxon );
        if ( res == null ) {
            return 0L;
        } else {
            return res;
        }
    }

    @JsonIgnore
    @Transient
    public Long getSize() {
        Long size = 0L;
        for ( Long count : this.sizesByTaxon.values() ) {
            size += count;
        }
        return size;
    }


}
