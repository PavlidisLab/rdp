package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
@Setter
@EqualsAndHashCode(of = { "uberonId" })
@ToString(of = { "uberonId" })
public abstract class Organ {

    @NaturalId
    @Column(name = "uberon_id", nullable = false)
    private String uberonId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;
}
