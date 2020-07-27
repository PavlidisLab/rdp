package ubc.pavlab.rdp.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@MappedSuperclass
@EqualsAndHashCode(of = { "symbol", "taxon" })
public abstract class Organ {

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column
    private String symbol;

    @Column
    private String name;

    @Column
    private String description;
}
