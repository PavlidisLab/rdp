package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "taxon")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class Taxon {

    @Id
    @Column(name = "taxon_id")
    private int id;

    private String scientificName;

    private String commonName;

    @JsonIgnore
    private String geneUrl;

    @JsonIgnore
    private boolean active;
}
