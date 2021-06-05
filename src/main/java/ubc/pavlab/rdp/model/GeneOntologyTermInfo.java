package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import ubc.pavlab.rdp.model.enums.RelationshipType;
import ubc.pavlab.rdp.services.GOService;

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

    /**
     * @deprecated please use {@link GOService#getSizeInTaxon} instead, this is only kept for the view layer.
     */
    @Deprecated
    public long size;

    @JsonIgnore
    private Collection<Relationship> parents = new HashSet<>();

    @JsonIgnore
    private Collection<Relationship> children = new HashSet<>();

    @JsonIgnore
    private Set<Integer> directGeneIds = new HashSet<>();

    @JsonIgnore
    private MultiValueMap<Integer, Integer> directGeneIdsByTaxonId = new LinkedMultiValueMap<>();
}
