package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Persistent organ information that can be copied over user organs.
 */
@Entity
@Table(name = "organ_info",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "uberon_id" }) })
@Getter
@Setter
public class OrganInfo extends Organ {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    @JsonIgnore
    private Integer id;

    @Column(nullable = false)
    @JsonIgnore
    private Boolean active;

    @Column
    @JsonIgnore
    private Integer ordering;
}
