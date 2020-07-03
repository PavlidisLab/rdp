package ubc.pavlab.rdp.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@MappedSuperclass
@EqualsAndHashCode(of = { "organId", "taxon" })
public abstract class Organ {

    @Column(name = "organ_id")
    private Integer organId;

    @ManyToOne
    @JoinColumn(name = "taxon_id")
    private Taxon taxon;

    @Column
    private String description;
}
