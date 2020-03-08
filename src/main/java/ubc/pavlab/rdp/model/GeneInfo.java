package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import javax.persistence.*;
import java.util.Set;

/**
 * Persistent gene metadata that can be copied over to user genes.
 */
@Entity
@Table(name = "gene_info",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = { "gene_id" }) },
        indexes = {
                @Index(columnList = "gene_id"),
                @Index(columnList = "gene_id, taxon_id"),
                @Index(columnList = "symbol, taxon_id") })
@Getter
@EqualsAndHashCode(of = "id")
public class GeneInfo extends Gene {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "ortholog",
            joinColumns = @JoinColumn(name = "source_gene"),
            inverseJoinColumns = @JoinColumn(name = "target_gene")
    )
    @JsonIgnore
    private Set<GeneInfo> orthologs;
}
