package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
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
public class GeneInfo extends Gene implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "ortholog",
            joinColumns = @JoinColumn(name = "source_gene"),
            inverseJoinColumns = @JoinColumn(name = "target_gene"))
    @JsonIgnore
    private Set<GeneInfo> orthologs = new HashSet<>();

}
