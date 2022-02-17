package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Immutable;

import javax.persistence.*;
import java.io.Serializable;
import java.net.URL;
import java.util.Comparator;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "taxon")
@Immutable
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = { "id" })
@ToString(of = { "id", "scientificName" })
public class Taxon implements Serializable {

    public static Comparator<Taxon> getComparator() {
        // taxon are ordered by the ordering field, however the ordering field is not set for remote users
        // because it is ignored in JSON serialization
        return Comparator.comparing( Taxon::getOrdering, Comparator.nullsLast( Comparator.naturalOrder() ) )
                .thenComparing( Taxon::getCommonName );
    }

    @Id
    @Column(name = "taxon_id")
    private Integer id;

    private String scientificName;

    private String commonName;

    @JsonIgnore
    private URL geneUrl;

    @JsonIgnore
    @Column(nullable = false)
    private boolean active;

    @JsonIgnore
    private Integer ordering;
}
