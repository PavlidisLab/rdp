package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.validator.constraints.Email;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Created by mjacobson on 19/01/18.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "token", "user" })
@ToString(of = { "token", "expiryDate" })
public class PasswordResetToken implements UserContent {

    public static final int EXPIRATION = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    @Column(name = "expiry_date")
    private Timestamp expiryDate;

    private Instant calculateExpiryDate( final int expiryTimeInHours ) {
        return Instant.now().plus( expiryTimeInHours, ChronoUnit.HOURS );
    }

    public void updateToken( final String token ) {
        this.token = token;
        this.expiryDate = Timestamp.from( calculateExpiryDate( EXPIRATION ) );
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
