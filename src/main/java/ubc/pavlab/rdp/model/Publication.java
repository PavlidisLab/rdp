package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;

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
    private Integer id;

    @NaturalId
    @Column(name = "pmid")
    private int pmid;

    @Column(name = "title")
    private String title;

}
