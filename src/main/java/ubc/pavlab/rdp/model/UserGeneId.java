package ubc.pavlab.rdp.model;

import lombok.*;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * Created by mjacobson on 17/01/18.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"user","gene"})
@ToString
public class UserGeneId implements Serializable {

    @ManyToOne(cascade = CascadeType.ALL)
    private User user;

    @ManyToOne(cascade = CascadeType.ALL)
    private Gene gene;
}
