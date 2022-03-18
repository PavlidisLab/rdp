package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = { "uberonId" })
@ToString(of = { "uberonId" })
public abstract class Organ {

    @NaturalId
    @Column(name = "uberon_id", length = 14)
    private String uberonId;

    @Column(columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
