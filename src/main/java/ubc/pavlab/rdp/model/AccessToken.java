package ubc.pavlab.rdp.model;

import lombok.Getter;
import lombok.Setter;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

@Entity
@Getter
@Setter
@Table(name = "access_token",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "token" }) })
public class AccessToken extends Token implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(optional = false)
    // FIXME: make this token non-nullable, see https://github.com/PavlidisLab/rdp/issues/166
    @JoinColumn(name = "user_id")
    private User user;

    @Override
    protected TemporalAmount getDuration() {
        return Duration.ofDays( 365 );
    }

    @Override
    public Optional<User> getOwner() {
        return Optional.of( user );
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return PrivacyLevelType.PRIVATE;
    }
}
