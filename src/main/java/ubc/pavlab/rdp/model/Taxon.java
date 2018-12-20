package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "taxon")
@Immutable
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "scientificName"})
public class Taxon {

    @Id
    @Column(name = "taxon_id")
    private Integer id;

    private String scientificName;

    private String commonName;

    @JsonIgnore
    private String geneUrl;

    @JsonIgnore
    private boolean active;

    private Integer ordering;
}
