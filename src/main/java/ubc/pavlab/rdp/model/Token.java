package ubc.pavlab.rdp.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.TemporalAmount;

@Getter
@Setter
@EqualsAndHashCode(of = { "token" })
@ToString(of = { "token", "expiryDate" })
@MappedSuperclass
public abstract class Token implements Serializable {

    @NaturalId
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    protected abstract TemporalAmount getDuration();

    private Instant calculateExpiryDate() {
        return Instant.now().plus( getDuration() );
    }

    public void updateToken( final String token ) {
        this.token = token;
        this.expiryDate = calculateExpiryDate();
    }
}
