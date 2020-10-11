package ubc.pavlab.rdp.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

@Data
@EqualsAndHashCode(of = { "token" })
@ToString(of = { "token", "expiryDate" })
@MappedSuperclass
public abstract class Token {

    @Column(name = "token", nullable = false)
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
