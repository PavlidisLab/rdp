package ubc.pavlab.rdp.model;

import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "publication")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"pmid"})
@ToString
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "publication_id")
    private int id;

    @Column(name = "pmid")
    private int pmid;

    @Column(name = "title")
    private String title;

    @Column(name = "url")
    private String url;

}
