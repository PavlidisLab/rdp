package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by mjacobson on 17/01/18.
 */
@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = { "geneId" })
@ToString(of = { "geneId", "symbol", "taxon" })
public abstract class Gene {

    @NaturalId
    @Column(name = "gene_id")
    private int geneId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "taxon_id", nullable = false)
    private Taxon taxon;

    @Column(name = "symbol", length = 63)
    private String symbol;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String name;

    @Lob
    @Column(name = "synonyms", columnDefinition = "TEXT")
    private String aliases;

    @Temporal(TemporalType.DATE)
    @Column(name = "modification_date")
    private Date modificationDate;
}
