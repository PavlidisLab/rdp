package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.security.PrivacySensitive;

import javax.persistence.*;
import java.util.Optional;

@Entity
@Table(name = "organ",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "symbol", "taxon_id" }) })
@EqualsAndHashCode(of = { "user", "symbol", "taxon" }, callSuper = false)
public class UserOrgan extends Organ implements PrivacySensitive {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Override
    public Optional<User> getOwner() {
        return Optional.of( user );
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return user.getEffectivePrivacyLevel();
    }
}
