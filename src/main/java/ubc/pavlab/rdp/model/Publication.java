package ubc.pavlab.rdp.model;

import lombok.*;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "publication")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "publication_id")
    private int id;

    @Column(name = "pmid")
    private int pubMedId;

    @Column(name = "title")
    private String title;

    @Column(name = "publication_date")
    private Date publicationDate;

    @Column(name = "fulltext_uri")
    private String fullTextUri;

    @Column(name = "abstract", columnDefinition = "TEXT")
    private String abstractText;
}
