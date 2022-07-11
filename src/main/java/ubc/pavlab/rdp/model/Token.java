package ubc.pavlab.rdp.model;

import lombok.*;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

@Getter
@Setter
@EqualsAndHashCode(of = { "token" })
@ToString(of = { "token", "expiryDate" })
@MappedSuperclass
public abstract class Token {

    @NaturalId
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Timestamp expiryDate;

    protected abstract TemporalAmount getDuration();

    private Instant calculateExpiryDate() {
        return Instant.now().plus( getDuration() );
    }

    public void updateToken( final String token ) {
        this.token = token;
        this.expiryDate = Timestamp.from( calculateExpiryDate() );
    }
}
