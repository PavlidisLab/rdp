package ubc.pavlab.rdp.model;

import lombok.*;

import javax.persistence.*;

/**
 * Created by mjacobson on 16/01/18.
 */
@Entity
@Table(name = "role")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "role_id")
    private int id;

    @Column(name = "role")
    private String role;
}
