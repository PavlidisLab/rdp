package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.enums.RelationshipType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(of = { "directGenes" }, callSuper = true)
public class GeneOntologyTermInfo extends GeneOntologyTerm {

    private boolean obsolete;

    @JsonIgnore
    private Collection<Relationship> parents = new HashSet<>();

    @JsonIgnore
    private Collection<Relationship> children = new HashSet<>();

    @JsonIgnore
    private Set<Integer> directGeneIds = new HashSet<>();

    @JsonIgnore
    private MultiValueMap<Integer, Integer> directGeneIdsByTaxonId = new LinkedMultiValueMap<>();

    @JsonIgnore
    public Collection<GeneOntologyTermInfo> getParents( boolean includePartOf ) {
        return parents.stream()
                .filter( r -> includePartOf || r.getType().equals( RelationshipType.IS_A ) )
                .map( Relationship::getTerm )
                .collect( Collectors.toSet() );
    }

    @JsonIgnore
    public Collection<GeneOntologyTermInfo> getAncestors( boolean includePartOf ) {
        Collection<GeneOntologyTermInfo> ancestors = new HashSet<>();

        for ( GeneOntologyTermInfo parent : getParents( includePartOf ) ) {
            ancestors.add( parent );
            ancestors.addAll( parent.getAncestors( includePartOf ) );
        }

        return ancestors;
    }
}
