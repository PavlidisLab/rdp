package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "publication")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "pmid" })
@ToString
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "publication_id")
    @JsonIgnore
    private Integer id;

    @Column(name = "pmid")
    private int pmid;

    @Column(name = "title")
    private String title;

}
