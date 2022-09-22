package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.NaturalId;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import javax.persistence.*;

/**
 * Created by mjacobson on 16/01/18.
 */
@Entity
@Table(name = "role")
@Immutable
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "role" })
@ToString
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "role_id")
    private Integer id;

    @NaturalId
    @Column(name = "role", unique = true)
    private String role;

    public DefaultMessageSourceResolvable getResolvableTitle() {
        return new DefaultMessageSourceResolvable( new String[]{ "Role." + role }, null, role );
    }
}
