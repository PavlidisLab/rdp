package ubc.pavlab.rdp.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Table(name = "access_token",
        uniqueConstraints = { @UniqueConstraint(columnNames = { "token" }) })
public class AccessToken extends Token implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreatedDate
    private Timestamp createdAt;

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
