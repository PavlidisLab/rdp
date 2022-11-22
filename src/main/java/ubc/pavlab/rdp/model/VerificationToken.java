package ubc.pavlab.rdp.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Email;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

/**
 * Created by mjacobson on 22/01/18.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "verification_token")
@Getter
@Setter
@NoArgsConstructor
@ToString(of = { "user" }, callSuper = true)
public class VerificationToken extends Token implements UserContent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Email address subject to verification.
     */
    @Email
    @Column(name = "email")
    private String email;

    @CreatedDate
    private Instant createdAt;

    @Override
    protected TemporalAmount getDuration() {
        return Duration.of( 24, ChronoUnit.HOURS );
    }

    @Override
    public Optional<User> getOwner() {
        return Optional.of( getUser() );
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return PrivacyLevelType.PRIVATE;
    }
}
