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

import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(of = { "directGeneIds" }, callSuper = true)
public class GeneOntologyTermInfo extends GeneOntologyTerm implements Comparable<GeneOntologyTermInfo> {

    public static Comparator<GeneOntologyTermInfo> getComparator() {
        return Comparator.comparing( GeneOntologyTermInfo::getSize, Comparator.reverseOrder() )
                .thenComparing( GeneOntologyTermInfo::getGoId );
    }

    private boolean obsolete;

    @Deprecated
    private final ThreadLocal<Long> size = new ThreadLocal<>();

    @JsonIgnore
    private Collection<Relationship> parents = new HashSet<>();

    @JsonIgnore
    private Collection<Relationship> children = new HashSet<>();

    @JsonIgnore
    private Set<Integer> directGeneIds = new HashSet<>();

    @JsonIgnore
    private MultiValueMap<Integer, Integer> directGeneIdsByTaxonId = new LinkedMultiValueMap<>();

    /**
     * Alias for {@link #getGoId()}.
     */
    @JsonIgnore
    public String getId() {
        return getGoId();
    }

    /**
     * Obtain the size of the term as a number of genes in a taxon.
     * <p>
     * The taxon is unknown and may change since this attribute is merely used as a temporary storage for the result of
     * a {@link GOService#getSizeInTaxon(GeneOntologyTermInfo, Taxon)} invocation.
     *
     * @deprecated use {@link GOService#getSizeInTaxon} instead and store the result of the computation elsewhere
     */
    @Deprecated
    public long getSize() {
        return size.get() != null ? size.get() : 0L;
    }

    @Deprecated
    public void setSize( long value ) {
        size.set( value );
    }

    @Override
    public int compareTo( GeneOntologyTermInfo other ) {
        return getComparator().compare( this, other );
    }
}
