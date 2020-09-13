package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = {"geneId"})
@ToString(of = {"geneId", "symbol", "taxon"})
public abstract class Gene {

    @Column(name = "gene_id")
    private int geneId;

    @ManyToOne
    @JoinColumn(name = "taxon_id", nullable = false)
    private Taxon taxon;

    @Column(name = "symbol", length = 63)
    private String symbol;

    @Column(name = "description", columnDefinition = "TEXT")
    private String name;

    @Column(name = "synonyms", columnDefinition = "TEXT")
    private String aliases;

    @Temporal(TemporalType.DATE)
    @Column(name = "modification_date")
    private Date modificationDate;
}
