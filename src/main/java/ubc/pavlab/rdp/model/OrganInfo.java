package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import javax.persistence.*;

@Entity
@Table(name = "organ_info",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "symbol", "taxon_id" }) })
@Getter
public class OrganInfo extends Organ {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;
}
