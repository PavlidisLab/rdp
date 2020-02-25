package ubc.pavlab.rdp.model;

import lombok.*;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;

import javax.persistence.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

/**
 * Created by mjacobson on 22/01/18.
 */
@Entity
@Table(name = "verification_token")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"token", "user"})
@ToString(of = {"user", "token", "expiryDate"})
public class VerificationToken implements PrivacySensitive {

    public static final int EXPIRATION = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    private String token;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private Date expiryDate;

    private Date calculateExpiryDate(final int expiryTimeInHours) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());
        cal.add(Calendar.HOUR, expiryTimeInHours);
        return new Date(cal.getTime().getTime());
    }

    public void updateToken(final String token) {
        this.token = token;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    @Override
    public Optional<User> getOwner() {
        return Optional.of(getUser());
    }

    @Override
    public PrivacyLevelType getEffectivePrivacyLevel() {
        return PrivacyLevelType.PRIVATE;
    }
}
