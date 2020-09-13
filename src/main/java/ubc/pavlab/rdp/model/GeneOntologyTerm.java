package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
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
@NoArgsConstructor
@EqualsAndHashCode(of = { "goId" })
@ToString(of = { "goId" })
public class GeneOntologyTerm {

    @Column(name = "go_id", length = 10)
    private String goId;

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
    private Map<Integer, Long> sizesByTaxonId = new HashMap<>();

    @JsonIgnore
    @Transient
    private Set<Integer> directGeneIds = new HashSet<>();

    public int getSize( Taxon taxon ) {
        Long size = this.sizesByTaxonId.get( taxon.getId() );
        if ( size == null ) {
            return 0;
        }
        return size.intValue();
    }

    @JsonIgnore
    @Transient
    public int getTotalSize() {
        return sizesByTaxonId.values().stream().mapToInt( Long::intValue ).sum();
    }

    public Collection<GeneOntologyTerm> getParents( boolean includePartOf ) {
        return parents.stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    public Collection<GeneOntologyTerm> getAncestors( boolean includePartOf ) {
        Collection<GeneOntologyTerm> ancestors = new HashSet<>();

        for ( GeneOntologyTerm parent : getParents( includePartOf ) ) {
            ancestors.add( parent );
            ancestors.addAll( parent.getAncestors( includePartOf ) );
        }

        return ancestors;
    }

}
